package org.flasck.flas.lsp;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.LanguageClient;

public interface FLASLanguageClient extends LanguageClient {
	@JsonNotification("flas/cardinfo")
	void sendCardInfo(Object webMetaData);
}
