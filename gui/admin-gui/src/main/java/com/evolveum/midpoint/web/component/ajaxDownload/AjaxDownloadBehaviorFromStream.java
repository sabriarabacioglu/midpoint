package com.evolveum.midpoint.web.component.ajaxDownload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Duration;

public abstract class AjaxDownloadBehaviorFromStream extends AbstractAjaxBehavior {

	private boolean addAntiCache;
	private String contentType = "text";

	public AjaxDownloadBehaviorFromStream() {
		this(true);
	}

	public AjaxDownloadBehaviorFromStream(boolean addAntiCache) {
		super();
		this.addAntiCache = addAntiCache;
	}

	/**
	 * Call this method to initiate the download.
	 */
	public void initiate(AjaxRequestTarget target) {
		String url = getCallbackUrl().toString();

		if (addAntiCache) {
			url = url + (url.contains("?") ? "&" : "?");
			url = url + "antiCache=" + System.currentTimeMillis();
		}

		// the timeout is needed to let Wicket release the channel
		target.appendJavaScript("setTimeout(\"window.location.href='" + url + "'\", 100);");
	}

	public void onRequest() {
		final byte[] byteStream = initStream();	
		IResourceStream resourceStream = new AbstractResourceStream(){

			InputStream stream;
			
			@Override
			public String getContentType() {
				return contentType;
			}

			@Override
			public InputStream getInputStream() throws ResourceStreamNotFoundException {
				stream = new ByteArrayInputStream(byteStream);
				return stream;
			}

			@Override
			public void close() throws IOException {
				stream.close();
				
			}
			
		};
		getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(
				new ResourceStreamRequestHandler(resourceStream) {
					@Override
					public void respond(IRequestCycle requestCycle) {
						super.respond(requestCycle);
					}
				}.setContentDisposition(ContentDisposition.ATTACHMENT)
						.setCacheDuration(Duration.ONE_SECOND));
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}


	protected abstract byte[] initStream();
}