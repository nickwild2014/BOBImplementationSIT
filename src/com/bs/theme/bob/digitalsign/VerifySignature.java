package com.bs.theme.bob.digitalsign;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;

public class VerifySignature {

	public static void main(String[] args) throws Exception {

		String sfmsMessage = "{A:BGSF01O760XXXKKBK0000958CNRB0004098112000TI362029260352432BGS2017081414592277875577XXXXXXXXX0958IGF17020154099}{4:"
				+ " :27:1/1"
				+ " :20:0958IGF170201540"
				+ " :23:ISSUE"
				+ " :30:20170407"
				+ " :40C:NONE"
				+ " :77C:FREE-FORMAT INSTRUCTIONS FOR MT760"
				+ " DOCUMENTS REQUIRED"
				+ " ADDITIONAL CONDITIONS -}"
				+ "";
		String encodedDigsignature = "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 6bNhCLhx7M85K31OjIGk4eQAAAAAAAA";
		Security.addProvider(new BouncyCastleProvider());
		CMSProcessable signedContent = new CMSProcessableByteArray(sfmsMessage.getBytes("ISO-8859-1"));
		// Create a InputStream object
		InputStream is = new ByteArrayInputStream(Base64.decode(encodedDigsignature.getBytes()));
		// Pass them both to CMSSignedData constructor
		CMSSignedData cms = new CMSSignedData(signedContent, is);
		Store store = cms.getCertificates();
		SignerInformationStore signers = cms.getSignerInfos();
		Collection c = signers.getSigners();
		Iterator it = c.iterator();
		while (it.hasNext()) {
			SignerInformation signer = (SignerInformation) it.next();
			Collection certCollection = store.getMatches(signer.getSID());
			Iterator certIt = certCollection.iterator();
			X509CertificateHolder certHolder = (X509CertificateHolder) certIt.next();
			X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
			if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert)))
				System.out.println("verified");
			else
				System.out.println("could not verified");
		}
	}

}
