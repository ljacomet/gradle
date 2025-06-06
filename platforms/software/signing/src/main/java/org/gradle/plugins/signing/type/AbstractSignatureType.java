/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.plugins.signing.type;

import org.gradle.internal.UncheckedException;
import org.gradle.plugins.signing.signatory.Signatory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Convenience base class for {@link SignatureType} implementations.
 */
public abstract class AbstractSignatureType implements SignatureType {

    @Override
    public File sign(Signatory signatory, File toSign) {
        File signatureFile = fileFor(toSign);
        try (InputStream toSignStream = new BufferedInputStream(new FileInputStream(toSign));
             OutputStream signatureFileStream = new BufferedOutputStream(new FileOutputStream(signatureFile))) {
            sign(signatory, toSignStream, signatureFileStream);
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
        return signatureFile;
    }

    @Override
    public void sign(Signatory signatory, InputStream toSign, OutputStream destination) {
        signatory.sign(toSign, destination);
    }

    @Override
    public File fileFor(File toSign) {
        return new File(toSign.getPath() + "." + getExtension());
    }

    @Override
    public String combinedExtension(File toSign) {
        String name = toSign.getName();
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex == -1 || dotIndex + 1 == name.length()) {
            return getExtension();
        } else {
            return name.substring(dotIndex + 1) + "." + getExtension();
        }
    }
}
