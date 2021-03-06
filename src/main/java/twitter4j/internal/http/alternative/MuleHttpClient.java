/**
 * (c) 2003-2015 MuleSoft, Inc. The software in this package is
 * published under the terms of the CPAL v1.0 license, a copy of which
 * has been included with this distribution in the LICENSE.md file.
 */

package twitter4j.internal.http.alternative; // NOSONAR

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.client.MuleClient;
import org.mule.api.transport.OutputHandler;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.modules.twitter.MuleHttpResponse;
import org.mule.transport.http.HttpConnector;
import twitter4j.TwitterException;
import twitter4j.internal.http.*;

import java.io.*;

import static twitter4j.internal.http.RequestMethod.POST;

/**
 * A version of the twitter4j client that uses Mule.
 */
public class MuleHttpClient implements HttpClient {

    private static MuleClient client;
    private static MuleContext context;
    private final HttpClientConfiguration httpConf;


    public MuleHttpClient(HttpClientConfiguration conf) {
        this.httpConf = conf;
    }

    public static void setMuleContext(MuleContext context) {
        MuleHttpClient.context = context;
        client = new DefaultLocalMuleClient(context);
    }

    @Override
    public HttpResponse request(final HttpRequest req) throws TwitterException {
        Object body = "";
        final boolean hasFile = HttpParameter.containsFile(req.getParameters());
        final String ctBoundary = "----Twitter4J-upload" + System.currentTimeMillis();
        final String boundary = "--" + ctBoundary;

        if (req.getMethod().equals(RequestMethod.POST)) {
            body = new OutputHandler() {

                @Override
                public void write(MuleEvent event, OutputStream os) throws IOException {
                    if (req.getMethod() == POST) {
                        if (hasFile) {
                            DataOutputStream out = new DataOutputStream(os);
                            for (HttpParameter param : req.getParameters()) {
                                if (param.isFile()) {
                                    out.writeBytes(boundary + "\r\n");
                                    out.writeBytes("Content-Disposition: form-data; name=\"" + param.getName() + "\"; filename=\"" + param.getFile().getName() + "\"\r\n");
                                    out.writeBytes("Content-Type: " + param.getContentType() + "\r\n\r\n");
                                    BufferedInputStream in = new BufferedInputStream(
                                            param.hasFileBody() ? param.getFileBody() : new FileInputStream(param.getFile())
                                    );
                                    int buff = 0;
                                    while ((buff = in.read()) != -1) {
                                        out.write(buff);
                                    }
                                    out.writeBytes("\r\n");
                                    in.close();
                                } else {
                                    out.writeBytes(boundary + "\r\n");
                                    out.writeBytes("Content-Disposition: form-data; name=\"" + param.getName() + "\"\r\n");
                                    out.writeBytes("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
                                    out.write(param.getValue().getBytes("UTF-8"));
                                    out.writeBytes("\r\n");
                                }
                            }
                            out.writeBytes(boundary + "--\r\n");
                            out.writeBytes("\r\n");

                        } else {
                            String postParam = HttpParameter.encodeParameters(req.getParameters());
                            byte[] bytes = postParam.getBytes("UTF-8");
                            os.write(bytes);
                        }
                        os.flush();
                        os.close();
                    }
                }
            };
        }
        DefaultMuleMessage msg = new DefaultMuleMessage(body, context);

        if (hasFile) {
            msg.setOutboundProperty("Content-Type", "multipart/form-data; boundary=" + ctBoundary);
        } else if (req.getMethod().equals(RequestMethod.POST)) {
            msg.setOutboundProperty("Content-Type",
                    "application/x-www-form-urlencoded");
        }
        msg.setOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, req.getMethod().name());
        String authorizationHeader;
        if (null != req.getAuthorization() && null != (authorizationHeader = req.getAuthorization().getAuthorizationHeader(req))) {
            msg.setOutboundProperty("Authorization", authorizationHeader);
        }
        try {

            HttpRequestOptions operationOptions = HttpRequestOptionsBuilder.newOptions().
                    method(req.getMethod().name()).build();
            return new MuleHttpResponse(httpConf, client.send(req.getURL(), msg, operationOptions));
        } catch (MuleException e) {
            throw new TwitterException(e);
        }
    }

    @Override
    public void shutdown() { // NOSONAR
    }


}
