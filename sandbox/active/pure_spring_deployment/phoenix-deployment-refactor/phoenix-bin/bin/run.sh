#! /bin/sh
################################################################
# Licensed to the Apache Software Foundation (ASF) under one   #
# or more contributor license agreements.  See the NOTICE file #
# distributed with this work for additional information        #
# regarding copyright ownership.  The ASF licenses this file   #
# to you under the Apache License, Version 2.0 (the            #
# "License"); you may not use this file except in compliance   #
# with the License.  You may obtain a copy of the License at   #
#                                                              #
#   http://www.apache.org/licenses/LICENSE-2.0                 #
#                                                              #
# Unless required by applicable law or agreed to in writing,   #
# software distributed under the License is distributed on an  #
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       #
# KIND, either express or implied.  See the License for the    #
# specific language governing permissions and limitations      #
# under the License.                                           #
################################################################
#
# -----------------------------------------------------------------------------
# Phoenix start script.
#

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

# resolve links - $0 may be a softlink
THIS_PROG="$0"

while [ -h "$THIS_PROG" ]; do
  ls=`ls -ld "$THIS_PROG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    THIS_PROG="$link"
  else
    THIS_PROG=`dirname "$THIS_PROG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$THIS_PROG"`
PHOENIX_HOME=`cd "$PRGDIR/.." ; pwd`

unset THIS_PROG

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$PHOENIX_HOME" ] && PHOENIX_HOME=`cygpath --unix "$PHOENIX_HOME"`
fi

$PHOENIX_HOME/bin/phoenix.sh run $*
