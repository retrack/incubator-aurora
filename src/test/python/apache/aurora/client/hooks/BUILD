#
# Copyright 2013 Apache Software Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

python_test_suite(name = 'all',
  dependencies = [
    pants(':hooked_api'),
    pants(':non_hooked_api'),
  ],
)

python_tests(name = 'hooked_api',
  sources = ['test_hooked_api.py'],
  dependencies = [
    pants('3rdparty/python:mock'),
    pants('src/main/python/apache/aurora/common:cluster'),
    pants('src/main/python/apache/aurora/client:api'),
    pants('src/main/python/apache/aurora/client/hooks'),
  ]
)

python_tests(name = 'non_hooked_api',
  sources = ['test_non_hooked_api.py'],
  dependencies = [
    pants('src/main/python/apache/aurora/common:aurora_job_key'),
    pants('src/main/python/apache/aurora/client/hooks'),
  ],
)
