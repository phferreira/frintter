package br.com.frintter.utils;

import org.opencv.core.Point;

/**
 * Created by paulo on 13/05/17.
 */

public class Coordenadas {
    private Point[] coordenada = new Point[20];

    public Point getCoordenada(int NumeroPonto) {
        return coordenada[NumeroPonto];
    }

    public void setCoordenada(Point coordenada, int NumeroPonto) {
        this.coordenada[NumeroPonto] = coordenada;
    }
}
