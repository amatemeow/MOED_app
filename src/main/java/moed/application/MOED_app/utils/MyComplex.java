package moed.application.MOED_app.utils;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.exception.NullArgumentException;

public class MyComplex extends Complex {

    public MyComplex(double real) {
        super(real);
    }

    public MyComplex(double real, double imaginary) {
        super(real, imaginary);
    }

    public MyComplex(Complex complex) {
        super(complex.getReal(), complex.getImaginary());
    }

    public double getComplexSpectrum() {
        return this.getReal() + this.getImaginary();
    }

    public double getErmitSpectrum() {
        return this.getReal() - this.getImaginary();
    }

    public double getAmplutideSpectrum() {
        return Math.sqrt(Math.pow(this.getReal(), 2) + Math.pow(this.getImaginary(), 2));
    }
    
    @Override
    public MyComplex divide(Complex divisor) throws NullArgumentException {
        return new MyComplex(super.divide(divisor));
    }

    @Override
    public MyComplex multiply(Complex factor) throws NullArgumentException {
        return new MyComplex(super.multiply(factor));
    }

    @Override
    public MyComplex multiply(double factor) throws NullArgumentException {
        return new MyComplex(super.multiply(factor));
    }
    
}
