package aidc.aigui.notebook;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

public class NotebookTokenizer 
{
	public  static final int TT_EOF      = -1;
	public  static final int TT_EOL      = '\n';
	public  static final int TT_NUMBER   = -2;
	public  static final int TT_WORD     = -3;
	private static final int TT_NOTHING  = -4;
	public  static final int TT_BOXDATA  = -5;
	public  static final int TT_DATA_END = -6;
	public  static final int TT_RULE     = -7;      /* -> */
	public  static final int TT_RULED    = -8;      /* :> */
	public  static final int TT_SLOTVAL  = -9;      /* # , #n, ##, ##n (n=0,1,2,...) */

	public  static final int TT_LE       = -10;      /* <= */
	public  static final int TT_EQ       = -11;      /* == */
	public  static final int TT_NE       = -12;     /* != */
	public  static final int TT_GE       = -13;     /* >= */
	public  static final int TT_AND      = -14;     /* && */
	public  static final int TT_OR       = -15;     /* || */
	public  static final int TT_PART_L   = -16;     /* [[ */
	public  static final int TT_PART_R   = -17;     /* ]] */
	public  static final int TT_ADDTO    = -18;     /* += */
	public  static final int TT_SUBFROM  = -19;     /* -= */
	public  static final int TT_TIMESBY  = -20;     /* *= */
	public  static final int TT_DIVBY    = -21;     /*/= */

	public  static final int TT_LT       = '<';
	public  static final int TT_GT       = '>';
	
	private static final int NEED_CHAR = Integer.MAX_VALUE;
	private static final int SKIP_LF   = Integer.MAX_VALUE - 1;

	public int ttype = TT_NOTHING;

	private int peekc  = NEED_CHAR;
	private int LINENO = 1;
	private boolean pushedBack;
	
	public String sval;
	public double nval;
	public double prec;

	private Reader        reader;
	private StringBuilder charBuf       = new StringBuilder();
	private int[]         putbackBuffer = new int[8];
	private int           putbackLength = 0;

	public void start(Reader reader)
	{
		if (reader == null) throw new NullPointerException();
		this.reader = reader;
		ttype = TT_NOTHING;
	}

	/** Read the next character */
	private int read() throws IOException 
	{
		if (putbackLength > 0) return putbackBuffer[--putbackLength];
		return reader.read();
	}

	public int getLineNumber() 
	{
		return LINENO;
	}

	/** put back characters in reverse order */
	private void pushback( int c )
	{
		if (putbackLength >= putbackBuffer.length)
		{
			putbackBuffer = Arrays.copyOf(putbackBuffer, 2*putbackBuffer.length);
		}
		putbackBuffer[putbackLength++] = c;
		
	}
	
	public int nextToken() throws IOException 
	{
		if (pushedBack) {
			pushedBack = false;
			return ttype;
		}
		sval = null;

		int c = peekc;
		if (c < 0)
			c = NEED_CHAR;
		if (c == SKIP_LF) {
			c = read();
			if (c < 0)
				return ttype = TT_EOF;
			if (c == '\n')
				c = NEED_CHAR;
		}
		if (c == NEED_CHAR) {
			c = read();
			if (c < 0)
				return ttype = TT_EOF;
		}
		ttype = c; /* Just to be safe */

		/* Set peekc so that the next invocation of nextToken will read
		 * another character unless peekc is reset in this invocation
		 */
		peekc = NEED_CHAR;

		//== skip white spaces
wsp:	for(;;)
		{
			switch(c)
			{
				case '\r':
					LINENO++;
					c = read();
					if (c == '\n') c = read();
					break;
				case '\n':
					LINENO++;
				case 0x09:  // tab
				case 0x0b:  // VT
				case 0x0c:  // FF
				case 0x20:  // space
					c = read();
					break;
				case '(':
				{
					int d = read();
					if (d != '*') 
					{
						pushback(d);
						break wsp;
					}
					c = read();
					while (c >= 0)
					{
						if (c == '*')
						{
							c = read();
							if (c == ')' || c < 0) break;
						}
						else
						{
							if (c=='\n') LINENO++;
							c = read();
						}
					}
					if (c >= 0) c = read();
					break;
				}
			
				case '\\':
				{
					int d = read();
					if (d == '\r' || d == '\n')
					{
						c = d;  // ignore continuation character
					}
					else
					{
						pushback(d); 
						break wsp;
					}
						
					break;
				}
				default: break wsp;
			}
		}
		if (c < 0) return ttype = TT_EOF;

		//== skip comments
		if (c=='(')
		{
			int d = read();
			if (d == '*') 
			{
				d = read();
				while (d >= 0)
				{
					if (d == '*')
					{
						int c2 = read();
						if (c2 == ')' || c2 < 0) break;
					}
					d = read();
				}
				c = read();
			}
			else
				pushback(d);
		}
		switch(c)
		{
			//== Parse number (no sign)
			case '.':
			case '0': case '1': case '2': case '3': case '4': 
	        case '5': case '6': case '7': case '8': case '9': 
	        {
				double mantissa  = 0.0;
				double precision = 0.0;
				int    decexp    = 0;
				int    seendot   = 0;
				for(;;) 
				{
					if (c == '.' && seendot == 0)
						seendot = 1;
					else if ('0' <= c && c <= '9') {
						mantissa = mantissa * 10.0 + (c - '0');
						decexp  -= seendot;
					} else
						break;
					c = read();
				}
				//== check for precision
				prec = -1.0;
				if (c=='`')
				{
					int precexp = 0;
					int preclen = 0;
					c = read();
					seendot = 0;
					while (c >= '0' && c <= '9' || c=='.')
					{
						if (c == '.')
						{
							if (++seendot > 1) break;
						}
						else
						{
							precision = precision * 10.0 + (c - '0');
							precexp  -= seendot;
						}
						preclen++;
						c=read();
					}
					if (preclen > 0)
						prec = buildDouble(precision, precexp);
				}
				//== check for exponent
				if (c=='*')
				{
					int d = read();
					if (d != '^')
					{
						pushback(d);
					}
					else
					{
						int eE = c;
						boolean negexp = false;
						c = read();
						if (c=='+') c = read();
						else if (c=='-') { negexp = true; c = read(); }
						if (c < '0' || c > '9')
						{
							pushback(c);
							c = eE;
						}
						else
						{
							int exp = 0;
							while (c >= '0' && c <= '9')
							{
								exp = exp * 10 + (c - '0');
								c = read();
							}
							if (negexp) exp = -exp;
							decexp += exp;
						}
						peekc = c;
					}
				}
				nval = buildDouble(mantissa,decexp);
				peekc = c;
				return ttype = TT_NUMBER;
	        }
	        
			//== QUOTED string token ==============================
	        case '"':
	        {
				ttype = c;
				charBuf.setLength(0);
				
				c = read();
				while (c >= 0 && c != ttype)
				{
					 //== keep the backslash and the sign after the backslash, maybe "
					if (c == '\\' )
					{
						charBuf.append((char)c);
						c = read();
						if (c < 0) break;
					}
					if (c=='\n') LINENO++;
					charBuf.append((char)c);
					c = read();
				}
				//== now c is the quote sign or EOF
				peekc = NEED_CHAR;
				sval = charBuf.toString();
				return ttype;
	        }
	        case '\\':
	        {
				int d = read();
				if (d == '(') 
				{
					int level = 0;
					d = read();
					charBuf.setLength(0);
					while (d >= 0)
					{
						if (d == '\\')
						{
							int c2 = read();
							if (c2 < 0) break;
							if (c2 == ')') { if (--level < 0) break; }
							if (c2 == '(') level++;
							charBuf.append((char)d);
							charBuf.append((char)c2);
						}
						else
						{
							charBuf.append((char)d);
						}
						d = read();
					}
					sval = charBuf.toString();
					return TT_BOXDATA;
				}
				if (d == ')') return TT_DATA_END;
				if (d == '\\') return c;
				pushback(d);
				break;
	        }
	        case '+':
			{
				int d = read();
				if (d == '=') return TT_ADDTO;
				pushback(d);
				break;
			}
	        case '-':
			{
				int d = read();
				if (d == '>') return TT_RULE;
				if (d == '=') return TT_SUBFROM;
				pushback(d);
				break;
			}
	        case ':':
			{
				int d = read();
				if (d == '>') return TT_RULED;
				pushback(d);
				break;
			}
	        case '*':
			{
				int d = read();
				if (d == '=') return TT_TIMESBY;
				pushback(d);
				break;
			}
	        case '/':
			{
				int d = read();
				if (d == '=') return TT_DIVBY;
				pushback(d);
				break;
			}
	        case '#':   // "#" [ "#" ] { digit }
			{
				charBuf.setLength(0);
				charBuf.append((char)c);
				c = read();
				if (c=='#') { charBuf.append((char)c); c = read(); }
				while (c >= '0' && c <= '9')  { charBuf.append((char)c); c = read(); }
				sval = charBuf.toString();
				peekc = c;
				return TT_SLOTVAL;
			}
	        case '[':
			{
				int d = read();
				if (d == '[') return TT_PART_L;
				pushback(d);
				break;
			}
/*	        case ']': may be in nested functions ! Introduce level of [
			{
				int d = read();
				if (d == ']') return TT_PART_R;
				pushback(d);
				break;
			}
*/	        case '<':
			{
				int d = read();
				if (d == '=') return TT_LE;
				pushback(d);
				break;
			}
			case '=':
			{
				int d = read();
				if (d == '=') return TT_EQ;
				pushback(d);
				break;
			}
			case '>':
			{
				int d = read();
				if (d == '=') return TT_GE;
				pushback(d);
				break;
			}
			case '&':
			{
				int d = read();
				if (d == '&') return TT_AND;
				pushback(d);
				break;
			}
			case '|':
			{
				int d = read();
				if (d == '|') return TT_OR;
				pushback(d);
				break;
			}
	        default:
	    		//== WORD token ==============================
	        	//    c>=0 && c<128  &&  mask >> c & 1 != 0
	        	if ( (c&(~127)) == 0 && ((alpha[c>>5] >> (c&31))&1) != 0)
	    		{
	    			charBuf.setLength(0);
	    			do {
	    				charBuf.append((char)c);
	    				c = read();
	    			} while ((c&(~127)) == 0 && ((alnum[c>>5] >> (c&31))&1) != 0 || c=='`');
	    			peekc = c;
	    			sval = charBuf.toString();
	    			return ttype = TT_WORD;
	    		}
		}
		return ttype = c;
	}
	//== Masks for testing character types
	private static final int alpha[] = {0x0, 0x0000010, 0x87FFFFFE, 0x7FFFFFE}; // $ A-Z a-z _
	private static final int alnum[] = {0x0, 0x3FF0010, 0x87FFFFFE, 0x7FFFFFE}; // $ A-Z a-z _ 0-9

	/**
	 * Computes a double from mantissa and exponent.
	 */
	static double buildDouble(double mant, int exp) 
	{
		if (exp < -324 || mant == 0.0) return 0f;
		if (exp == 0)  return mant;
		if (exp > 308) return (mant > 0) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;

		double fexp = 1.0;
		int    iexp = (exp > 0) ? exp : -exp;  
		{
			if (iexp < pow10.length)
				fexp = pow10[iexp];
			else
			{
				fexp = pow10[pow10.length-1];
				iexp = iexp - pow10.length + 1;
				while (iexp > 0) { fexp *= 10.0; --iexp; }
			}
		}
		return (exp > 0) ? mant * fexp : mant / fexp; 
	}

	/**
	 * Array of powers of ten for fast access with exponents until 24.
	 */
	private static final double pow10[] = { 1.0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11,
		1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19, 1e20, 1e21, 1e22, 1e23};

	public String toString() 
	{
		String ret;
		switch (ttype) {
		case TT_EOF:
			ret = "EOF";
			break;
		case TT_EOL:
			ret = "EOL";
			break;
		case TT_WORD:
			ret = sval;
			break;
		case TT_NUMBER:
			ret = "n=" + nval;
			break;
		case TT_BOXDATA:
			ret = "\\(";
			break;
		case TT_DATA_END:
			ret = "\\)";
			break;
		case TT_RULE:
			ret = "->";
			break;
		case TT_RULED:
			ret = ":>";
			break;
		case TT_SLOTVAL:
			ret = sval; 
			break;
		case TT_PART_L:
			ret = " [[ ";
			break;
		case TT_PART_R:
			ret = " ]] ";
			break;
		case TT_NOTHING:
			ret = "NOTHING";
			break;
		default: {
			/* 
			 * ttype is the first character of either a quoted string or
			 * is an ordinary character. ttype can definitely not be less
			 * than 0, since those are reserved values used in the previous
			 * case statements
			 */
			if (ttype < 256 && ttype == '"') {
				ret = sval;
				break;
			}

			char s[] = new char[3];
			s[0] = s[2] = '\'';
			s[1] = (char) ttype;
			ret = new String(s);
			break;
		}
		}
		return "Token[" + ret + "], line " + LINENO;
	}
}
