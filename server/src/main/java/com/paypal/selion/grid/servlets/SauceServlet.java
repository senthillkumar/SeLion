/*-------------------------------------------------------------------------------------------------------------------*\
|  Copyright (C) 2014-2016 PayPal                                                                                          |
|                                                                                                                     |
|  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance     |
|  with the License.                                                                                                  |
|                                                                                                                     |
|  You may obtain a copy of the License at                                                                            |
|                                                                                                                     |
|       http://www.apache.org/licenses/LICENSE-2.0                                                                    |
|                                                                                                                     |
|  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed   |
|  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for  |
|  the specific language governing permissions and limitations under the License.                                     |
\*-------------------------------------------------------------------------------------------------------------------*/

package com.paypal.selion.grid.servlets;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.paypal.selion.pojos.SeLionGridConstants;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.web.servlet.RegistryBasedServlet;
import org.openqa.selenium.remote.internal.HttpClientFactory;

import com.paypal.selion.utils.ServletHelper;

/**
 * This {@link RegistryBasedServlet} based servlet automatically takes care of spinning off a virtual node when it gets
 * initialized. It also gives the end user an opportunity to re-spawn the same virtual node again explicitly by doing a
 * GET/POST against the URL : <code>http://{hub-host}:{hub-port}/grid/admin/SauceServlet</code>.
 * <br>
 * <br>
 * This requires the hub to also have {@link LoginServlet} available. 
 */
public class SauceServlet extends RegistryBasedServlet {

    private static final long serialVersionUID = 9187677490975386050L;
    private static int count;

    public SauceServlet(Registry registry) {
        super(registry);
    }

    public SauceServlet() {
        this(null);
    }

    /**
     * A helper method that takes care of registering a virtual node to the hub.
     */
    public void registerVirtualSauceNodeToGrid(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Redirecting to login page if session is not found
        if (req.getSession(false) == null) {
            resp.sendRedirect(LoginServlet.class.getSimpleName());
            return;
        }

        URL registration;
        try {

            String respMsg = "<p align='center'><b>Sauce node already registered</b></p>";
            if (count > 0) {
                ServletHelper.respondAsHtmlWithMessage(resp, respMsg);
                return;
            }

            int port = getRegistry().getHub().getPort();
            registration = new URL("http://localhost:" + port + "/grid/register");

            BasicHttpEntityEnclosingRequest r = new BasicHttpEntityEnclosingRequest("POST",
                    registration.toExternalForm());
            String json = FileUtils.readFileToString(new File(SeLionGridConstants.NODE_SAUCE_CONFIG_FILE));
            r.setEntity(new StringEntity(json));
            HttpHost host = new HttpHost(registration.getHost(), registration.getPort());
            HttpClientFactory httpClientFactory = new HttpClientFactory();
            HttpClient client = httpClientFactory.getHttpClient();
            HttpResponse response = client.execute(host, r);
            // TODO: Should we do something with the status code for e.g., check if it was "200". Maybe attempt a few
            // retries as well if the registration didn't go through ?

            respMsg = "<p align='center'><b>Sauce node registration failed. Please refer to the log file for failure details</b></p>";

            if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
                respMsg = "<p align='center'><b>Sauce node registered successfully</b></p>";
            }

            ServletHelper.respondAsHtmlWithMessage(resp, respMsg);
            count++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        registerVirtualSauceNodeToGrid(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        registerVirtualSauceNodeToGrid(req, resp);
    }

}