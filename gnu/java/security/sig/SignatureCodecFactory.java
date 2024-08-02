/* SignatureCodecFactory.java -- Factory to instantiate Signature codecs
   Copyright (C) 2006, 2014, 2015 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */


package gnu.java.security.sig;

import gnu.java.security.Registry;
import gnu.java.security.hash.HashFactory;
import gnu.java.security.sig.dss.DSSSignatureRawCodec;
import gnu.java.security.sig.dss.DSSSignatureX509Codec;
import gnu.java.security.sig.rsa.RSAPKCS1V1_5SignatureRawCodec;
import gnu.java.security.sig.rsa.RSAPKCS1V1_5SignatureX509Codec;
import gnu.java.security.sig.rsa.RSAPSSSignatureRawCodec;
import gnu.java.security.util.FormatUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A <i>Factory</i> class to instantiate Signature codecs.
 */
public class SignatureCodecFactory
{
  private static Set<String> names;

  /** Trivial constructor to enforce Singleton pattern. */
  private SignatureCodecFactory()
  {
    super();
  }

  /**
   * Returns the appropriate codec given a composed signature algorithm and an
   * encoding format. A composed name is formed by the concatenation of the
   * canonical signature algorithm name, the forward slash character
   * <code>/</code> and the canonical name of the encoding format.
   * <p>
   * When the encoding format name is missing, the Raw encoding format is
   * assumed. When this is the case the trailing forward slash is discarded from
   * the name.
   *
   * @param name the case-insensitive, possibly composed, signature codec name.
   * @return an instance of the signaturecodec, or <code>null</code> if none
   *         found.
   */
  public static ISignatureCodec getInstance(String name)
  {
    if (name == null)
      return null;

    name = name.trim();
    if (name.length() == 0)
      return null;

    if (name.startsWith("/"))
      return null;

    if (name.endsWith("/"))
      return getInstance(name.substring(0, name.length() - 1),
                         Registry.RAW_ENCODING_ID);

    int i = name.indexOf("/");
    if (i == - 1)
      return getInstance(name, Registry.RAW_ENCODING_ID);

    String sigName = name.substring(0, i);
    String formatName = name.substring(i + 1);
    return getInstance(sigName, formatName);
  }

  /**
   * Returns an instance of a signature codec given the canonical name of the
   * signature algorithm, and that of the encoding format.
   *
   * @param name the case-insensitive signature algorithm name.
   * @param format the name of the format to use when encodigng/decoding
   *          signatures generated by the named algorithm.
   * @return an instance of the signature codec, or <code>null</code> if none
   *         found.
   */
  public static ISignatureCodec getInstance(String name, String format)
  {
    int formatID = FormatUtil.getFormatID(format);
    if (formatID == 0)
      return null;

    return getInstance(name, formatID);
  }

  /**
   * Returns an instance of a signature codec given the canonical name of the
   * signature algorithm, and the identifier of the format to use when
   * encoding/decoding signatures generated by that algorithm.
   *
   * @param name the case-insensitive signature algorithm name.
   * @param formatID the identifier of the format to use when encoding /
   *          decoding signatures generated by the designated algorithm.
   * @return an instance of the signature codec, or <code>null</code> if none
   *         found.
   */
  public static ISignatureCodec getInstance(String name, int formatID)
  {
    if (name == null)
      return null;

    name = name.trim();
    switch (formatID)
      {
      case Registry.RAW_ENCODING_ID:
        return getRawCodec(name);
      case Registry.X509_ENCODING_ID:
        return getX509Codec(name);
      default:
	return null;
      }
  }

  /**
   * Returns a {@link Set} of supported signature codec names.
   *
   * @return a {@link Set} of the names of supported signature codec (Strings).
   */
  public static synchronized final Set<String> getNames()
  {
    if (names == null)
      {
        HashSet<String> hs = new HashSet<String>();
        hs.add(Registry.DSS_SIG + "/" + Registry.RAW_ENCODING_SHORT_NAME);
        hs.add(Registry.DSS_SIG + "/" + Registry.X509_ENCODING_SORT_NAME);
	for (String mdName : HashFactory.getNames())
          {
            String name = Registry.RSA_PKCS1_V1_5_SIG + "-" + mdName;
            hs.add(name + "/" + Registry.RAW_ENCODING_SHORT_NAME);
            hs.add(name + "/" + Registry.X509_ENCODING_SORT_NAME);
            name = Registry.RSA_PSS_SIG + "-" + mdName;
            hs.add(name + "/" + Registry.RAW_ENCODING_SHORT_NAME);
          }

        names = Collections.unmodifiableSet(hs);
      }

    return names;
  }

  /**
   * @param name the trimmed name of a signature algorithm.
   * @return a Raw format codec for the designated signature algorithm, or
   *         <code>null</code> if none exists.
   */
  private static ISignatureCodec getRawCodec(String name)
  {
    ISignatureCodec result = null;
    if (name.equalsIgnoreCase(Registry.DSA_SIG)
        || name.equalsIgnoreCase(Registry.DSS_SIG))
      result = new DSSSignatureRawCodec();
    else
      {
        name = name.toLowerCase();
        if (name.startsWith(Registry.RSA_PKCS1_V1_5_SIG))
          result = new RSAPKCS1V1_5SignatureRawCodec();
        else if (name.startsWith(Registry.RSA_PSS_SIG))
          result = new RSAPSSSignatureRawCodec();
      }

    return result;
  }

  /**
   * @param name the trimmed name of a signature algorithm.
   * @return a X.509 format codec for the designated signature algorithm, or
   *         <code>null</code> if none exists.
   */
  private static ISignatureCodec getX509Codec(String name)
  {
    ISignatureCodec result = null;
    if (name.equalsIgnoreCase(Registry.DSA_SIG)
        || name.equalsIgnoreCase(Registry.DSS_SIG))
      result = new DSSSignatureX509Codec();
    else
      {
        name = name.toLowerCase();
        if (name.startsWith(Registry.RSA_PKCS1_V1_5_SIG))
          result = new RSAPKCS1V1_5SignatureX509Codec();
      }

    return result;
  }
}
