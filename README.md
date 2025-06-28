# SimpleJ IDE Plugin

A simple IntelliJ IDEA Plugin for teaching purposes and performing basic engineering tasks.

<img src="https://raw.githubusercontent.com/wildsmith/simplej-plugin/refs/heads/main/logo.svg" alt="SimpleJ Logo" width="150">

SimpleJ should not be viewed as a utility in and of itself but rather a mechanism to inspire ideas for better DevEx while providing code samples.

## Features

Functionality currently offered includes...
- [Configurable `json`](#simplej-config-json) attributes for workspace validation, new module templates and browser overlays
- *Some* customization of the Plugin through the [Settings](#settings) panel
- [New module creation](#new-module-creation) using templates specified within `simplej-config.json`
- Safe [module deletion](#safe-module-deletion), plus removal of any entries within `CODEOWNERS`
- Location-aware [Gradle Task execution](#gradle-task-execution)
- [Open in Github](#open-in-github--copy-github-link)
- [Copying the Github link](#open-in-github--copy-github-link) for the current file and/or file line range
- [Code owner lookup](#code-owner-lookup) for the current file/directory
- [Workspace validation](#workspace-validation) (ssh, java version/home, Android build tools) based on the values defined within 
  `simplej-config.json`
- Nested IDE [browser overlay](#browser-overlay) based on the values defined within `simplej-config.json`

### SimpleJ Config Json

Configuration file that informs various SimpleJ attributes, including:
- New module templates
- Web Browser overlay mappings
- Workspace validation checks...
    - ssh
    - java version/home
    - android build tools

Located at: [`config` > `simplej` > `simplej-config.json`](https://github.com/wildsmith/simplej-plugin/blob/main/config/simplej/simplej-config.json)

```json
{
  "workspaceCompat": {
    "ssh": {},
    "java": {},
    "android": {}
  },
  "webBrowserMappings": {},
  "newModuleTemplates": []
}
```

### Settings

- Easily accessible through SimpleJ’s Popup Menu Actions
- Customized by `simplej-config.json`
- Allows for storing plugin State changes
- Provides a visual surface for `simplej-config.json`

<img src="/docs/images/settings-flow.png" alt="settings flow" width="600px"/>

### New Module Creation

**W/out SimpleJ**
- Utilize Intellij/Android Studio new module templates
- Fill in gaps for additional metadata; `CODEOWNERS`
- Update prefabbed files for project-specific capabilities; convention plugin’s, DSL configuration, etc
- Manually Gradle Sync

**W/SimpleJ**

Right click > SimpleJ > New Module

<img src="/docs/images/new-module-flow.png" alt="new module flow" width="600px"/>

### Safe Module Deletion

**W/out SimpleJ**
- Delete the module’s directory
- Manually delete references to the module from disparate locations; `CODEOWNERS`, `settings.gradle.kts`, `.gitignore`
- Ensure no dependencies exist on the project

**W/SimpleJ**

Right click > SimpleJ > Delete Module

<img src="/docs/images/module-deletion-flow.png" alt="module deletion flow" width="600px"/>

### Gradle Task Execution

**W/out SimpleJ**
- Within terminal navigate to the appropriate project directory
- Concat lengthy Gradle paths together for task execution
- Remember arbitrary task options & properties

**W/SimpleJ**

Right click > SimpleJ > Run... > ...

<img src="/docs/images/gradle-task-execution-flow.png" alt="gradle task execution flow" width="600px"/>

### Open in Github + Copy Github Link

**W/out SimpleJ**
- Identify a file’s repository and branch
- Open a browser tab and manually navigate the repository
- When sharing code, manually copy the tab’s url
- Specific line navigation requires additional steps

**W/SimpleJ**

Right click > SimpleJ > Open in Github

<img src="/docs/images/github-interaction-flow.png" alt="github interaction flow" width="600px"/>

### Code Owner Lookup

**W/out SimpleJ**
- Search `CODEOWNERS` for parts of the file path
- Ownership claims can use wildcards, making manual searching inaccurate
- Order matters, so finding the last entry is important
- `CODEOWNER` files can be very noisy

**W/SimpleJ**

Right click > SimpleJ > Lookup Code Owner

<img src="/docs/images/code-owner-lookup.png" alt="code owner lookup" width="600px"/>

### Workspace Validation

**W/out SimpleJ**
- Misconfigured or missing environment variables
- Back and forth with support engineers
- Often even the best documentation can result in missed steps
- Errors appearing as they’re encountered

**W/SimpleJ**

Right click > SimpleJ > Validate Workspace

<img src="/docs/images/workspace-validation.png" alt="workspace validation" width="600px"/>

### Browser Overlay

**W/out SimpleJ**
- Manually finding & opening relevant documentation websites
- Context switching between the browser and the IDE
- No clear or preferred doc websites for frameworks, projects, or files

**W/SimpleJ**

Open mapped file > Click the globe icon

## Install

SimpleJ is available on the [Plugin Marketplace](https://plugins.jetbrains.com/plugin/27739-simplej-teaching-aid).

SimpleJ can also be installed locally by downloading the `plugin-x.x.x.zip` located [here](https://github.com/wildsmith/simplej-plugin/blob/main/artifact). Then navigating to `Settings` > `Plugins` > ⚙️ > `Install Plugin from Disk...` and selecting the downloaded zip.

## Validating Changes

When modifying SimpleJ's functionality the fastest way to validate changes is to push the changes to a development instance of the IDE. This is done through the use of the following command:
```shell
./gradlew :plugin:runIde
```

### Debugging

To debug Plugin behaviors run that same command with some additional flags, like so...
```shell
./gradlew :plugin:runIde --debug-jvm
```

Then trigger the debugger within your IDE using a 'Remote JVM Debug' run configuration.

## License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
