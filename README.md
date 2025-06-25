# SimpleJ IDE Plugin

A simple IntelliJ IDEA Plugin for teaching purposes and performing basic engineering tasks.

<img src="https://raw.githubusercontent.com/wildsmith/simplej-plugin/refs/heads/main/logo.svg" alt="SimpleJ Logo" width="150">

SimpleJ should not be viewed as a utility in and of itself but rather a mechanism to inspire ideas for better DevEx while providing code samples.

## Features

Functionality currently offered includes...
<ul>
<li>Configurable <code>json</code> attributes for workspace validation, new module templates and browser overlays</li>
<li>*Some* customization of the Plugin through the Settings panel</li>
<li>New module creation using templates specified within <code>simplej-config.json</code></li>
<li>Safe module deletion, plus removal of any entries within CODEOWNERS</li>
<li>Location-aware Gradle Task execution</li>
<li>Open a file and/or file line range within Github</li>
<li>Copying the Github link for the current file and/or file line range</li>
<li>Code owner lookup for the current file/directory</li>
<li>Workspace validation (ssh, java version/home, Android build tools) based on the values defined within <code>simplej-config.json</code></li>
<li>Nested IDE browser overlay based on the values defined within <code>simplej-config.json</code></li>
</ul>

## Install

SimpleJ is available on the [Plugin Marketplace](https://plugins.jetbrains.com/plugin/27739-simplej-teaching-aid) and can be installed locally by 
downloading the `artifact.zip` located [here](https://github.com/wildsmith/simplej-plugin/blob/main/artifact/plugin-1.0.2.zip).

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