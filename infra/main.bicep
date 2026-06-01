targetScope = 'subscription'

@minLength(1)
@maxLength(64)
@description('Name of the the environment which is used to generate a short unique hash used in all resources.')
param environmentName string

@minLength(1)
@description('Primary location for all resources')
@allowed(['australiaeast', 'eastasia', 'eastus', 'eastus2', 'northeurope', 'southcentralus', 'southeastasia', 'swedencentral', 'uksouth', 'westus2', 'eastus2euap'])
@metadata({
  azd: {
    type: 'location'
  }
})
param location string
param vnetEnabled bool

@description('References application or service contact information from a Service or Asset Management database')
param serviceManagementReference string = ''

@description('Comma-separated list of client application IDs to pre-authorize for accessing the MCP API (optional)')
param preAuthorizedClientIds string = ''

@description('Enable Microsoft Entra ID authentication (Easy Auth) for the deployed function app')
param enableAuth bool = true

param serviceName string = ''
param userAssignedIdentityName string = ''
param applicationInsightsName string = ''
param appServicePlanName string = ''
param logAnalyticsName string = ''
param resourceGroupName string = ''
param storageAccountName string = ''
param vNetName string = ''
param disableLocalAuth bool = true

var abbrs = loadJsonContent('./abbreviations.json')
var resourceToken = toLower(uniqueString(subscription().id, environmentName, location))
var tags = { 'azd-env-name': environmentName }
var functionAppName = !empty(serviceName) ? serviceName : '${abbrs.webSitesFunctions}${resourceToken}'
var deploymentStorageContainerName = 'app-package-${take(toLower(functionAppName), 32)}-${take(toLower(uniqueString(functionAppName, resourceToken)), 7)}'

// Convert comma-separated string to array for pre-authorized client IDs
var preAuthorizedClientIdsArray = !empty(preAuthorizedClientIds) ? map(split(preAuthorizedClientIds, ','), clientId => trim(clientId)) : []

// Organize resources in a resource group
resource rg 'Microsoft.Resources/resourceGroups@2021-04-01' = {
  name: !empty(resourceGroupName) ? resourceGroupName : '${abbrs.resourcesResourceGroups}${environmentName}'
  location: location
  tags: tags
}

// User assigned managed identity for the function app
module userAssignedIdentity './core/identity/userAssignedIdentity.bicep' = {
  name: 'userAssignedIdentity'
  scope: rg
  params: {
    location: location
    tags: tags
    identityName: !empty(userAssignedIdentityName) ? userAssignedIdentityName : '${abbrs.managedIdentityUserAssignedIdentities}${resourceToken}'
  }
}

// App Service Plan (Flex Consumption)
module appServicePlan './core/host/appserviceplan.bicep' = {
  name: 'appserviceplan'
  scope: rg
  params: {
    name: !empty(appServicePlanName) ? appServicePlanName : '${abbrs.webServerFarms}${resourceToken}'
    location: location
    tags: tags
    sku: {
      name: 'FC1'
      tier: 'FlexConsumption'
    }
  }
}

// Entra ID application registration for authentication (optional, controlled by enableAuth)
module entraApp 'app/entra.bicep' = if (enableAuth) {
  name: 'entraApp'
  scope: rg
  params: {
    appUniqueName: '${functionAppName}-app'
    appDisplayName: 'MCP Authorization App (${functionAppName})'
    serviceManagementReference: serviceManagementReference
    functionAppHostname: '${functionAppName}.azurewebsites.net'
    preAuthorizedClientIds: preAuthorizedClientIdsArray
    managedIdentityClientId: userAssignedIdentity.outputs.identityClientId
    managedIdentityPrincipalId: userAssignedIdentity.outputs.identityPrincipalId
    tags: tags
  }
}

// Function App
module api './app/api.bicep' = {
  name: 'api'
  scope: rg
  params: {
    name: functionAppName
    location: location
    tags: tags
    applicationInsightsName: monitoring.outputs.applicationInsightsName
    appServicePlanId: appServicePlan.outputs.id
    runtimeName: 'java'
    runtimeVersion: '17'
    storageAccountName: storage.outputs.name
    deploymentStorageContainerName: deploymentStorageContainerName
    identityId: userAssignedIdentity.outputs.identityId
    identityClientId: userAssignedIdentity.outputs.identityClientId
    serviceName: 'mcp'
    appSettings: {}
    virtualNetworkSubnetId: !vnetEnabled ? '' : serviceVirtualNetwork!.outputs.appSubnetID
    // Authorization parameters (passed only when auth is enabled)
    authClientId: enableAuth ? entraApp!.outputs.applicationId : ''
    authIdentifierUri: enableAuth ? entraApp!.outputs.identifierUri : ''
    authExposedScopes: enableAuth ? entraApp!.outputs.exposedScopes : []
    authTenantId: enableAuth ? tenant().tenantId : ''
    preAuthorizedClientIds: preAuthorizedClientIdsArray
  }
}

// Backing storage for Azure functions
module storage './core/storage/storage-account.bicep' = {
  name: 'storage'
  scope: rg
  params: {
    name: !empty(storageAccountName) ? storageAccountName : '${abbrs.storageStorageAccounts}${resourceToken}'
    location: location
    tags: tags
    containers: [{name: deploymentStorageContainerName}, {name: 'snippets'}]
    publicNetworkAccess: vnetEnabled ? 'Disabled' : 'Enabled'
    networkAcls: !vnetEnabled ? {} : {
      defaultAction: 'Deny'
    }
  }
}

var StorageBlobDataOwner = 'b7e6dc6d-f1e8-4753-8033-0f276bb0955b'
var StorageQueueDataContributor = '974c5e8b-45b9-4653-ba55-5f855dd0fb88'

// Allow access from app to blob storage using a managed identity
module blobRoleAssignment 'app/storage-Access.bicep' = {
  name: 'blobRoleAssignment'
  scope: rg
  params: {
    storageAccountName: storage.outputs.name
    roleDefinitionID: StorageBlobDataOwner
    principalID: userAssignedIdentity.outputs.identityPrincipalId
  }
}

// Allow access from app to queue storage using a managed identity
module queueRoleAssignment 'app/storage-Access.bicep' = {
  name: 'queueRoleAssignment'
  scope: rg
  params: {
    storageAccountName: storage.outputs.name
    roleDefinitionID: StorageQueueDataContributor
    principalID: userAssignedIdentity.outputs.identityPrincipalId
  }
}

// Virtual Network & private endpoint to blob storage
module serviceVirtualNetwork 'app/vnet.bicep' =  if (vnetEnabled) {
  name: 'serviceVirtualNetwork'
  scope: rg
  params: {
    location: location
    tags: tags
    vNetName: !empty(vNetName) ? vNetName : '${abbrs.networkVirtualNetworks}${resourceToken}'
  }
}

module storagePrivateEndpoint 'app/storage-PrivateEndpoint.bicep' = if (vnetEnabled) {
  name: 'servicePrivateEndpoint'
  scope: rg
  params: {
    location: location
    tags: tags
    virtualNetworkName: !empty(vNetName) ? vNetName : '${abbrs.networkVirtualNetworks}${resourceToken}'
    subnetName: !vnetEnabled ? '' : serviceVirtualNetwork!.outputs.peSubnetName
    resourceName: storage.outputs.name
  }
}

// Monitor application with Azure Monitor
module monitoring './core/monitor/monitoring.bicep' = {
  name: 'monitoring'
  scope: rg
  params: {
    location: location
    tags: tags
    logAnalyticsName: !empty(logAnalyticsName) ? logAnalyticsName : '${abbrs.operationalInsightsWorkspaces}${resourceToken}'
    applicationInsightsName: !empty(applicationInsightsName) ? applicationInsightsName : '${abbrs.insightsComponents}${resourceToken}'
    disableLocalAuth: disableLocalAuth  
  }
}

var monitoringRoleDefinitionId = '3913510d-42f4-4e42-8a64-420c390055eb' // Monitoring Metrics Publisher role ID

// Allow access from app to application insights using a managed identity
module appInsightsRoleAssignment './core/monitor/appinsights-access.bicep' = {
  name: 'appInsightsRoleAssignment'
  scope: rg
  params: {
    appInsightsName: monitoring.outputs.applicationInsightsName
    roleDefinitionID: monitoringRoleDefinitionId
    principalID: userAssignedIdentity.outputs.identityPrincipalId
  }
}

// App outputs
output AZURE_LOCATION string = location
output AZURE_TENANT_ID string = tenant().tenantId
output SERVICE_MCP_NAME string = api.outputs.SERVICE_API_NAME
output SERVICE_MCP_DEFAULT_HOSTNAME string = api.outputs.SERVICE_MCP_DEFAULT_HOSTNAME
output AZURE_FUNCTION_NAME string = api.outputs.SERVICE_API_NAME
output AZURE_RESOURCE_GROUP string = rg.name

// Entra App outputs (only when auth is enabled)
output ENTRA_APPLICATION_ID string = enableAuth ? entraApp!.outputs.applicationId : ''
output ENTRA_APPLICATION_OBJECT_ID string = enableAuth ? entraApp!.outputs.applicationObjectId : ''
output ENTRA_SERVICE_PRINCIPAL_ID string = enableAuth ? entraApp!.outputs.servicePrincipalId : ''
output ENTRA_IDENTIFIER_URI string = enableAuth ? entraApp!.outputs.identifierUri : ''

// Authorization outputs
output AUTH_ENABLED bool = enableAuth ? api.outputs.AUTH_ENABLED : false
output CONFIGURED_SCOPES string = enableAuth ? api.outputs.CONFIGURED_SCOPES : ''
output PRE_AUTHORIZED_CLIENT_IDS string = preAuthorizedClientIds
