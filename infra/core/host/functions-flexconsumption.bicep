param name string
param location string = resourceGroup().location
param tags object = {}

// Reference Properties
param applicationInsightsName string = ''
param appServicePlanId string
param storageAccountName string
param virtualNetworkSubnetId string = ''
@allowed(['SystemAssigned', 'UserAssigned'])
param identityType string
@description('User assigned identity name')
param identityId string

// Runtime Properties
@allowed([
  'dotnet-isolated', 'node', 'python', 'java', 'powershell', 'custom'
])
param runtimeName string
@allowed(['3.10', '3.11', '7.4', '8.0', '10', '11', '17', '20'])
param runtimeVersion string
param kind string = 'functionapp,linux'

// Microsoft.Web/sites/config
param appSettings object = {}
param instanceMemoryMB int = 2048
param maximumInstanceCount int = 100
param deploymentStorageContainerName string

// Authorization parameters
@description('The Entra ID application (client) ID for App Service Authentication')
param authClientId string = ''

@description('The Entra ID identifier URI for App Service Authentication')
param authIdentifierUri string = ''

@description('The Azure AD tenant ID for App Service Authentication')
param authTenantId string = ''

@description('The client ID of the user-assigned managed identity')
param identityClientId string = ''

@description('OAuth2 delegated permissions for App Service Authentication login flow')
param delegatedPermissions array = ['User.Read']

@description('Client application IDs to pre-authorize for the default scope')
param preAuthorizedClientIds array = []

var enableAuth = !empty(authClientId) && !empty(authTenantId)

// Auth-specific app settings
var authAppSettings = enableAuth && !empty(authIdentifierUri) && !empty(identityClientId) ? {
  WEBSITE_AUTH_PRM_DEFAULT_WITH_SCOPES: '${authIdentifierUri}/user_impersonation'
  OVERRIDE_USE_MI_FIC_ASSERTION_CLIENTID: identityClientId
  WEBSITE_AUTH_AAD_ALLOWED_TENANTS: authTenantId
} : {}

resource stg 'Microsoft.Storage/storageAccounts@2022-09-01' existing = {
  name: storageAccountName
}

resource functions 'Microsoft.Web/sites@2023-12-01' = {
  name: name
  location: location
  tags: tags
  kind: kind
  identity: {
    type: identityType
    userAssignedIdentities: { 
      '${identityId}': {}
    }
  }
  properties: {
    serverFarmId: appServicePlanId
    functionAppConfig: {
      deployment: {
        storage: {
          type: 'blobContainer'
          value: '${stg.properties.primaryEndpoints.blob}${deploymentStorageContainerName}'
          authentication: {
            type: identityType == 'SystemAssigned' ? 'SystemAssignedIdentity' : 'UserAssignedIdentity'
            userAssignedIdentityResourceId: identityType == 'UserAssigned' ? identityId : '' 
          }
        }
      }
      scaleAndConcurrency: {
        instanceMemoryMB: instanceMemoryMB
        maximumInstanceCount: maximumInstanceCount
      }
      runtime: {
        name: runtimeName
        version: runtimeVersion
      }
    }
    virtualNetworkSubnetId: !empty(virtualNetworkSubnetId) ? virtualNetworkSubnetId : null
  }

  resource configAppSettings 'config' = {
    name: 'appsettings'
    properties: union(appSettings, authAppSettings,
      {
        AzureWebJobsStorage__accountName: stg.name
        AzureWebJobsStorage__credential : 'managedidentity'
        APPLICATIONINSIGHTS_CONNECTION_STRING: applicationInsights.properties.ConnectionString
      })
  }
}

// Configure App Service Authentication (Easy Auth) when auth is enabled
resource authSettings 'Microsoft.Web/sites/config@2023-12-01' = if (enableAuth) {
  parent: functions
  name: 'authsettingsV2'
  properties: {
    globalValidation: {
      requireAuthentication: true
      unauthenticatedClientAction: 'Return401'
      redirectToProvider: 'azureactivedirectory'
    }
    httpSettings: {
      requireHttps: true
      routes: {
        apiPrefix: '/.auth'
      }
      forwardProxy: {
        convention: 'NoProxy'
      }
    }
    identityProviders: {
      azureActiveDirectory: {
        enabled: true
        registration: {
          openIdIssuer: '${environment().authentication.loginEndpoint}${authTenantId}/v2.0'
          clientId: authClientId
          clientSecretSettingName: 'OVERRIDE_USE_MI_FIC_ASSERTION_CLIENTID'
        }
        login: {
          loginParameters: [
            'scope=openid profile email ${join(delegatedPermissions, ' ')}'
          ]
        }
        validation: {
          jwtClaimChecks: {}
          allowedAudiences: [
            authIdentifierUri
          ]
          defaultAuthorizationPolicy: {
            allowedPrincipals: {}
            allowedApplications: union([authClientId], preAuthorizedClientIds)
          }
        }
        isAutoProvisioned: false
      }
    }
    login: {
      routes: {
        logoutEndpoint: '/.auth/logout'
      }
      tokenStore: {
        enabled: true
        tokenRefreshExtensionHours: 72
        fileSystem: {}
        azureBlobStorage: {}
      }
      preserveUrlFragmentsForLogins: false
      allowedExternalRedirectUrls: []
      cookieExpiration: {
        convention: 'FixedTime'
        timeToExpiration: '08:00:00'
      }
      nonce: {
        validateNonce: true
        nonceExpirationInterval: '00:05:00'
      }
    }
    platform: {
      enabled: true
      runtimeVersion: '~1'
    }
  }
}

resource applicationInsights 'Microsoft.Insights/components@2020-02-02' existing = {
  name: applicationInsightsName
}

output name string = functions.name
output defaultHostName string = functions.properties.defaultHostName
output uri string = 'https://${functions.properties.defaultHostName}'
output identityPrincipalId string = identityType == 'SystemAssigned' ? functions.identity.principalId : ''
