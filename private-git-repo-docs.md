# Private Git Repository Support

The Firefly Plugin Manager now supports installing plugins from private Git repositories. This feature allows you to securely access and install plugins from your organization's private repositories.

## Configuration

You can configure authentication for private Git repositories in your application properties.

For basic authentication:
- firefly.plugin-manager.git.authentication-type=basic
- firefly.plugin-manager.git.username=your-username
- firefly.plugin-manager.git.password=your-password

For token-based authentication (recommended):
- firefly.plugin-manager.git.authentication-type=token
- firefly.plugin-manager.git.access-token=your-personal-access-token

For SSH authentication (limited support):
- firefly.plugin-manager.git.authentication-type=ssh
- firefly.plugin-manager.git.private-key-path=/path/to/your/private-key
- firefly.plugin-manager.git.private-key-passphrase=your-passphrase

Other Git settings:
- firefly.plugin-manager.git.default-branch=main
- firefly.plugin-manager.git.timeout-seconds=60
- firefly.plugin-manager.git.verify-ssl=true

## Supported Authentication Types

- `none`: No authentication (for public repositories)
- `basic`: Username and password authentication
- `token`: Personal access token authentication (recommended for GitHub, GitLab, etc.)
- `ssh`: SSH key-based authentication (limited support, see note below)

### Note on SSH Authentication

SSH authentication is currently not fully supported. If you configure SSH authentication, the system will log a warning message and proceed without authentication. For private repositories, we recommend using token-based authentication instead:

- For GitHub: Use a personal access token (https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)
- For GitLab: Use a personal access token (https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html) or deploy token (https://docs.gitlab.com/ee/user/project/deploy_tokens/)
- For Bitbucket: Use an app password (https://support.atlassian.com/bitbucket-cloud/docs/app-passwords/)

## Example Usage

Installing a plugin from a private repository works the same way as with public repositories. You can use the following code:

```
URI privateRepoUri = URI.create("https://github.com/your-org/private-plugin.git");
PluginDescriptor descriptor = pluginManager.installPluginFromGit(privateRepoUri).block();
pluginManager.startPlugin(descriptor.metadata().id()).block();
```

The plugin manager will automatically use the configured authentication when cloning the repository.

## Advanced Examples

### Using Environment Variables for Credentials

For better security, you can use environment variables to store your credentials:

```
# In your application.properties
firefly.plugin-manager.git.authentication-type=token
firefly.plugin-manager.git.access-token=${GIT_ACCESS_TOKEN}
```

### Specifying a Branch

You can specify a branch when installing a plugin:

```
URI repoUri = URI.create("https://github.com/your-org/plugin.git");
String branch = "develop";
PluginDescriptor descriptor = pluginManager.installPluginFromGit(repoUri, branch).block();
```

## Security Considerations

- Store sensitive credentials securely, preferably in environment variables or a secure vault
- Use token-based authentication instead of username/password when possible
- Consider using read-only tokens with the minimum required permissions
- Regularly rotate access tokens for enhanced security
- For GitHub and GitLab, create tokens with the minimum required scopes (e.g., read-only access to repositories)
- Consider using deploy tokens or machine users for automated systems
- Monitor access logs for suspicious activity