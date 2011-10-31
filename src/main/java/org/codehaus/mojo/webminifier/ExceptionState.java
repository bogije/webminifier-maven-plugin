package org.codehaus.mojo.webminifier;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

/**
 * An error reporter.
 */
public class ExceptionState
{
    private boolean hasErrors = false;

    private boolean hasWarnings = false;

    /**
     * @return true if there are errors.
     */
    public boolean hasErrors()
    {
        return hasErrors;
    }

    /**
     * @return true if there are warnings.
     */
    public boolean hasWarnings()
    {
        return hasWarnings;
    }

    /**
     * Show that there are errors.
     */
    public void signalErrors()
    {
        this.hasErrors = true;
    }

    /**
     * Show that there are warnings.
     */
    public void signalWarnings()
    {
        this.hasWarnings = true;
    }

}