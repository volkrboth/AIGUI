package aidc.aigui.resources;
/* $Id$
 *
 * :Title: aigui
 *
 * :Description: Graphical user interface for Analog Insydes
 *
 * :Author:
 *   Adam Pankau
 *   Dr. Volker Boos <volker.boos@imms.de>
 *
 * :Copyright:
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU General Public License
 *   as published by the Free Software Foundation; either version 2
 *   of the License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.regex.Pattern;

/**
 * Class responsible for formating and parsing Mathematica numbers.
 * @author Volker Boos
 *
 */
public class MathematicaFormat extends DecimalFormat
{
	private static final long    serialVersionUID = 1L;
	private static final Pattern MathPow10 = Pattern.compile("\\*\\^");
	private static final Pattern JavaPow10 = Pattern.compile("E");

	/**
	 * Class constructor.
	 */
	public MathematicaFormat() 
	{
		super("0.##E0");
	}

	/**
	 * Method converts a double value to a Mathematica's string representation.
	 * @param d number to convert.
	 * @return string representation of a number.
	 */
	public String formatMath(double d)
	{
		return JavaPow10.matcher(super.format(d)).replaceAll("*^");
	}
	/**
	 * Method converts a Complex value to a Mathematica's string representation.
	 * @param c complex number to convert.
	 * @return string representation of a number.
	 */
	public String formatMath(Complex c) 
	{
		StringBuilder sb = new StringBuilder();
		if (c.re() != 0.0)
			sb.append( JavaPow10.matcher(super.format(c.re())).replaceAll("*^") );
		else if (c.im() == 0.0)
			sb.append('0');
		
		if (c.im() != 0.0)
		{
			if (c.im() > 0.0 && c.re()!=0.0) sb.append('+');
			sb.append(JavaPow10.matcher(super.format(c.im())).replaceAll("*^") );
			sb.append('I');
		}
		return sb.toString();
	}
	
	/**
	 * Method converts a Mathematica's string representation to a double value.
	 * @param s string to convert.
	 * @return double value.
	 */
	public double parseMath(String s)
	{
		try {
			return parseMathToNumber(s).doubleValue();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public Number parseMathToNumber(String s) throws ParseException
	{
		return super.parse(MathPow10.matcher(s).replaceFirst("E"));
	}
	
	/**
	 * Method converts a Mathematica's string representation to a complex value.
	 * @param s string to convert.
	 * @return complex value.
	 */
	public Complex parseMathToComplex(String s)
	{
		ParsePosition pp = new ParsePosition(0);
		double re = 0, im = 0;
		s = s.replaceAll(" ","");
		s = MathPow10.matcher(s).replaceAll("E").trim();
		Number num = super.parse(s, pp); 
		if (num == null) return null;
		re = num.doubleValue();
		if (s.length() > pp.getIndex())
		{
			if(s.charAt(pp.getIndex())=='I'||s.charAt(pp.getIndex())=='i')
			{
				im=re;
				re=0;
			}
			else
			{
				if(s.charAt(pp.getIndex())!='-')
					pp.setIndex(pp.getIndex() + 1);
				num = super.parse(s, pp);
				if (num == null) return null;
				im = num.doubleValue();
			}
		}
		return new Complex(re, im);
	}
}
