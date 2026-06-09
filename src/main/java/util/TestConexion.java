/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import com.mediconsult.beta.util.ConexionDB;

import java.sql.Connection;

public class TestConexion {

    public static void main(String[] args) {

        try {

            Connection con = ConexionDB.getInstance().getConexion();

            System.out.println(
                    "Conexion exitosa a MySQL"
            );

        } catch (Exception e) {

            System.out.println(
                    "Error de conexion"
            );

            e.printStackTrace();
        }
    }
}
