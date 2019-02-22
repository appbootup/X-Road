/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.auth;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Helper for handling encoded authentication headers
 */
@Component
public class AuthenticationHeaderDecoder {

    private static final String UPPERCASE_APIKEY_PREFIX = "X-ROAD-APIKEY TOKEN=";

    /**
     * Returns decoded api key from authorization header,
     * or IllegalArgumentException if one was not found
     * @param authenticationHeader
     * @return
     */
    public String decodeApiKey(String authenticationHeader) {
        if (authenticationHeader == null
                || authenticationHeader.toUpperCase().indexOf(UPPERCASE_APIKEY_PREFIX) != 0) {
            throw new IllegalArgumentException("Invalid X-Road-Apikey authorization header");
        }
        String apiKey = authenticationHeader.substring(UPPERCASE_APIKEY_PREFIX.length());
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalArgumentException("Missing api key from authorization header");
        }
        return apiKey.trim();
    }
}
