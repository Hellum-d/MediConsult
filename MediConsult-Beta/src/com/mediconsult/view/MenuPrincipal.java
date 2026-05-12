/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mediconsult.view;

import java.util.Scanner;

public class MenuPrincipal {
    private Scanner sc = new Scanner(System.in);

    public void mostrar() {
        int op;
        do {
            System.out.println("\n===== MEDICONSULT =====");
            System.out.println("1. Gestionar Pacientes");
            System.out.println("2. Gestionar Medicos");
            System.out.println("3. Gestionar Citas");
            System.out.println("4. Gestionar Diagnosticos");
            System.out.println("0. Salir");
            System.out.print("Seleccione: ");
            op = sc.nextInt();

            switch (op) {
                case 1: new MenuPacientes().mostrar(); break;
                case 2: new MenuMedicos().mostrar(); break;
                case 3: new MenuCitas().mostrar(); break;
                case 4: new MenuDiagnostico().mostrar(); break;
                case 0: System.out.println("Hasta luego."); break;
                default: System.out.println("Opcion no valida.");
            }
        } while (op != 0);
    }

    public static void main(String[] args) {
        new MenuPrincipal().mostrar();
    }
}