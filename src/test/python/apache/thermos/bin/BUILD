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
    pants(':small'),
  ]
)

python_test_suite(name = 'small',
  dependencies = [
    pants(':test_thermos'),
  ]
)

python_tests(name = 'test_thermos',
  sources = ['test_thermos.py'],
  dependencies = [
     pants('src/main/python/apache/thermos/bin:thermos'),
     pants('src/main/python/apache/thermos/testing:runner'),
  ]
)
