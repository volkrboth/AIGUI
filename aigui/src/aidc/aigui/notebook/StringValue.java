package aidc.aigui.notebook;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class StringValue extends Value implements Actor
{
	String escValue; // all special characters are escaped (e.g. tab = "\t")
	String rawValue; // all characters are raw             (e.g. tab = 0x09)

	public StringValue(String sval, boolean escaped) 
	{
		if (escaped)
			escValue = sval;
		else
			rawValue = sval;
	}

	public String getRawValue()
	{
		if (rawValue == null)
		{
			rawValue = unescapeString(escValue);
		}
		return rawValue;
	}

	public String getEscValue()
	{
		if (escValue == null)
		{
			escValue = escapeString(rawValue);
		}
		return escValue;
	}

	@Override
	public String toString()
	{
		return "\"" + getEscValue() + "\"";
	}

	public static String escapeString(String s)
	{
		StringWriter sw = new StringWriter();
		try {
			writeEscaped(sw,s);
		} catch (IOException e) {}
		return sw.toString();
	}

	public static void writeEscaped(Writer out, String str) throws IOException
	{
		int sz;
		sz = str.length();
		for (int i = 0; i < sz; i++) 
		{
			char ch = str.charAt(i);
			// handle unicode
			if (ch > 0xfff) {
				out.write("\\u" + hex(ch));
			} else if (ch > 0xff) {
				out.write("\\u0" + hex(ch));
			} else if (ch > 0x7f) {
				out.write("\\u00" + hex(ch));
			} else if (ch < 32) {
				switch (ch) {
				case '\b':
					out.write('\\');
					out.write('b');
					break;
				case '\n':
					out.write('\\');
					out.write('n');
					break;
				case '\t':
					out.write('\\');
					out.write('t');
					break;
				case '\f':
					out.write('\\');
					out.write('f');
					break;
				case '\r':
					out.write('\\');
					out.write('r');
					break;
				default :
					if (ch > 0xf) {
						out.write("\\u00" + hex(ch));
					} else {
						out.write("\\u000" + hex(ch));
					}
					break;
				}
			} else {
				switch (ch) {
				case '"':
					out.write('\\');
					out.write('"');
					break;
				case '\\':
					out.write('\\');
					if (i < str.length()-1)
					{
						char d = str.charAt(i+1);
						if (d == '[' || d=='<' || d=='>' || d=='\n') break;
						if (d=='\r' && i < str.length()-2 && str.charAt(i+1) == '\n')
						{
							out.write('\r');
							i++;
							break;
						}
					}
					out.write('\\');
					break;
				default :
					out.write(ch);
					break;
				}
			}
		}
	}

	private static String hex(char ch) 
	{
		return Integer.toHexString(ch).toUpperCase();
	}

	private static final int ASC0 = '0';
	private static final int ASC7 = '7';
			
	public static String unescapeString(String s)
	{
		int ie = s.indexOf('\\');
		if (ie<0) return s; // no escapes in string

		StringBuilder sb = new StringBuilder();
		int i = 0; // index of input string
		for (i=0; i< s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '\\')
			{
				i++;
				if (i>=s.length()) break;
				int a = s.charAt(i);
				
				if (a >= ASC0 && a <= ASC7)                        /* octal number */ 
				{
					a = a - ASC0;                                  /* first digit */
					int first = a;                                 /* To allow \377, but not \477 */
					int b = (++i) < s.length() ? s.charAt(i) : -1; /* second digit */
					if (ASC0 <= b && b <= ASC7) 
					{
						a = (a << 3) + (b - ASC0);                 /* a = value of first and second digit */
						b = (++i) < s.length() ? s.charAt(i) : -1; /* third digit */
						if (ASC0 <= b && b <= ASC7 && first <= 3) 
						{
							a = (a << 3) + (b - ASC0);             /* a = value of all three digits */
							i++;
						}
					}
					c = (char)a;
					--i; // put back char after octal number
				}
				else  /* control character */
				{
					switch (c) {
					case 'a':
						c = 0x7;
						break;
					case 'b':
						c = '\b';
						break;
					case 'f':
						c = 0xC;
						break;
					case 'n':
						c = '\n';
						break;
					case 'r':
						c = '\r';
						break;
					case 't':
						c = '\t';
						break;
					case 'v':
						c = 0xB;
						break;
					default:
						sb.append('\\'); /* keep backslash */
						break;
					}
				}
				sb.append(c);
			}
		}
		return sb.toString();
	}

	@Override
	public boolean accept(Visitor visitor) throws VisitorException
	{
		return visitor.visitStringValue(this);
	}

}
