#######################################################################
## Copyright (c) 2017 Contributors to the Eclipse Foundation
##
## See the NOTICE file(s) distributed with this work for additional
## information regarding copyright ownership.
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##     http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
#######################################################################
#!/usr/bin/env bash

asciidoctor -o target/generated-docs/microprofile-fault-tolerance-spec.html src/main/asciidoc/microprofile-fault-tolerance-spec.asciidoc

bundle exec guard
