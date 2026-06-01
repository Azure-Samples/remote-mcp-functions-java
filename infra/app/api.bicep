param name string
param location string = resourceGroup().location
param tags object = {}
param applicationInsightsName string = ''
param appServicePlanId string
param appSettings object = {}
param runtimeName string 
param runtimeVersion string 
param serviceName string = 'mcp'
param storageAccountName string
param deploymentStorageContainerName string
param virtualNetworkSubnetId string = ''
param instanceMemoryMB int = 2048
param maximumInstanceCount int = 100
param identityId string = ''
param identityClientId string = ''

// Authorization parameters
@description('The Entra ID application (client) ID for App Service Authentication')
param authClientId string = ''

@description('The Entra ID identifier URI for App Service Authentication')
param authIdentifierUri string = ''

@description('The OAuth2 scopes exposed by the application for App Service Authentication')
param authExposedScopes array = []

@description('The Azure AD tenant ID for App Service Authentication')
param authTenantId string = ''

@description('OAuth2 delegated permissions for App Service Authentication login flow')
param delegatedPermissions array = ['User.Read']

@description('Client application IDs to pre-authorize for the default scope')
param preAuthorizedClientIds array = []

var applicationInsightsIdentity = 'ClientId=${identityClientId};Authorization=AAD'

module api '../core/host/functions-flexconsumption.bicep' = {
  name: '${serviceName}-functions-module'
  params: {
    name: name
    location: location
    tags: union(tags, { 'azd-service-name': serviceName })
    identityType: 'UserAssigned'
    identityId: identityId
    appSettings: union(appSettings,
      {
        AzureWebJobsStorage__clientId : identityClientId
        APPLICATIONINSIGHTS_AUTHENTICATION_STRING: applicationInsightsIdentity
      })
    applicationInsightsName: applicationInsightsName
    appServicePlanId: appServicePlanId
    runtimeName: runtimeName
    runtimeVersion: runtimeVersion
    storageAccountName: storageAccountName
    deploymentStorageContainerName: deploymentStorageContainerName
    virtualNetworkSubnetId: virtualNetworkSubnetId
    instanceMemoryMB: instanceMemoryMB 
    maximumInstanceCount: maximumInstanceCount
    authClientId: authClientId
    authIdentifierUri: authIdentifierUri
    authTenantId: authTenantId
    identityClientId: identityClientId
    delegatedPermissions: delegatedPermissions
    preAuthorizedClientIds: preAuthorizedClientIds
  }
}

output SERVICE_API_NAME string = api.outputs.name
output SERVICE_API_IDENTITY_PRINCIPAL_ID string = api.outputs.identityPrincipalId
output SERVICE_MCP_DEFAULT_HOSTNAME string = api.outputs.defaultHostName

// Authorization outputs
var scopeValues = [for scope in authExposedScopes: scope.value]
output AUTH_ENABLED bool = !empty(authClientId) && !empty(authTenantId)
output CONFIGURED_SCOPES string = !empty(authExposedScopes) ? join(scopeValues, ','): ''
