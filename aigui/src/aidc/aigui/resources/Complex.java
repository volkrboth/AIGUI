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

/**
 * This class represents complex numbers.
 * 
 * @author pankau
 */
public class Complex {
    private double re, im;

    /**
     * Class constructor.
     * 
     * @param re
     *            real part of a complex number
     * @param im
     *            imaginary part of a complex number
     */
    public Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    /**
     * Returns real part of a complex number.
     * 
     * @return real part of a complex number.
     */
    public double re() {
        return re;
    }

    /**
     * Returns imaginary part of a complex number.
     * 
     * @return imaginary part of a complex number.
     */
    public double im() {
        return im;
    }

    /**
     * Method checks if this object is equal to Complex object given as a
     * parameter
     * 
     * @param c
     *            object
     * @return true if object are equal, false otherwise.
     */
    public boolean equals(Complex c) {
        if (this.re == c.re() && this.im == c.im())
            return true;
        return false;
    }

    /**
     * Sets the imaginary part of a Complex object.
     * 
     * @param im
     *            The im to set.
     */
    public void setIm(double im) {
        this.im = im;
    }

    /**
     * Sets the real part of a Complex object.
     * 
     * @param re
     *            The re to set.
     */
    public void setRe(double re) {
        this.re = re;
    }

	/**
	 * Calculates the absolute value of Complex object
	 * @return  the absolute value
	 */
	public double abs() 
	{
		if (re == 0.0)
			return Math.abs(im);
		else if (im==0.0)
			return Math.abs(re);

		return Math.sqrt( re*re + im*im );
	}
}