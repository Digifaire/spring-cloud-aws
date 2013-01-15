/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.support.converter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.elasticspring.messaging.Message;
import org.elasticspring.messaging.StringMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 *
 */
public class ObjectMessageConverter implements MessageConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectMessageConverter.class);
	private static final String DEFAULT_ENCODING = "UTF-8";
	private final Charset encoding;

	public ObjectMessageConverter(String encoding) throws UnsupportedCharsetException {
		this.encoding = Charset.forName(encoding);
	}

	public ObjectMessageConverter() {
		this.encoding = Charset.forName(DEFAULT_ENCODING);
	}

	@Override
	public Message<String> toMessage(Object payload) {
		if (!(payload instanceof Serializable)) {
			throw new IllegalArgumentException("Can't convert payload, it must be of type Serializable");
		}

		ByteArrayOutputStream content = new ByteArrayOutputStream();
		Base64OutputStream base64OutputStream = new Base64OutputStream(content, true, 0, null);
		ObjectOutputStream objectOutputStream = null;
		try {
			objectOutputStream = new ObjectOutputStream(base64OutputStream);
			objectOutputStream.writeObject(payload);
			objectOutputStream.flush();
		} catch (IOException e) {
			throw new MessageConversionException("Error converting payload into binary representation", e);
		} finally {
			if (objectOutputStream != null) {
				try {
					objectOutputStream.close();
				} catch (IOException e) {
					LOGGER.warn("Error closing object output stream while writing message payload", e);
				}
			}
		}

		String messagePayload = new String(content.toByteArray(), 0, content.size(), this.encoding);

		return new StringMessage(messagePayload);

	}

	@Override
	public Serializable fromMessage(Message<String> message) {
		String messagePayload = message.getPayload();
		byte[] rawContent = messagePayload.getBytes(this.encoding);
		if (!(Base64.isArrayByteBase64(rawContent))) {
			throw new MessageConversionException("Error converting payload '" + messagePayload + "' because it is not a valid base64 encoded stream!");
		}
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(rawContent);
		Base64InputStream base64InputStream = new Base64InputStream(byteArrayInputStream);
		Serializable result = null;
		ObjectInputStream objectInputStream = null;
		try {
			objectInputStream = new ObjectInputStream(base64InputStream);
			result = (Serializable) objectInputStream.readObject();
		} catch (ClassNotFoundException e) {
			throw new MessageConversionException("Error loading class from message payload, make sure class is in classpath!", e);
		} catch (IOException e) {
			throw new MessageConversionException("Error reading payload from binary representation", e);
		} finally {
			if (objectInputStream != null) {
				try {
					objectInputStream.close();
				} catch (IOException e) {
					LOGGER.warn("Error closing object output stream while reading message payload", e);
				}
			}
		}

		return result;
	}
}