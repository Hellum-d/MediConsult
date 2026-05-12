/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mediconsult.view;

import com.mediconsult.dao.impl.DiagnosticoDAOImpl;
import com.mediconsult.model.Diagnostico;
import java.util.Optional;
import java.util.Scanner;

public class MenuDiagnostico {
    private DiagnosticoDAOImpl dao = new DiagnosticoDAOImpl();
    private Scanner sc = new Scanner(System.in);

    public void mostrar() {
        int op;
        do {
            System.out.println("\n--- Menu Diagnosticos ---");
            System.out.println("1. Agregar diagnostico");
            System.out.println("2. Listar diagnosticos");
            System.out.println("3. Buscar por cita");
            System.out.println("0. Volver");
            System.out.print("Seleccione: ");
            op = sc.nextInt();

            switch (op) {
                case 1:
                    System.out.print("ID: "); int id = sc.nextInt();
                    System.out.print("ID Cita: "); int citaId = sc.nextInt();
                    System.out.print("Sintomas: "); String sintomas = sc.next();
                    System.out.print("Resultado IA: "); String resultado = sc.next();
                    dao.guardar(new Diagnostico(id, citaId, sintomas, resultado));
                    System.out.println("Diagnostico agregado.");
                    break;
                case 2:
                    dao.listarPorPaciente(0).forEach(d -> System.out.println(d));
                    break;
                case 3:
                    System.out.print("ID Cita: ");
                    Optional<Diagnostico> d = dao.buscarPorCita(sc.nextInt());
                    System.out.println(d.isPresent() ? d.get() : "No encontrado.");
                    break;
            }
        } while (op != 0);
    }
}