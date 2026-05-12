/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mediconsult.view;

import com.mediconsult.dao.impl.PacienteDAOImpl;
import com.mediconsult.model.Paciente;
import java.util.Optional;
import java.util.Scanner;

public class MenuPacientes {
    private PacienteDAOImpl dao = new PacienteDAOImpl();
    private Scanner sc = new Scanner(System.in);

    public void mostrar() {
        int op;
        do {
            System.out.println("\n--- Menu Pacientes ---");
            System.out.println("1. Agregar paciente");
            System.out.println("2. Listar pacientes");
            System.out.println("3. Buscar paciente");
            System.out.println("4. Eliminar paciente");
            System.out.println("0. Volver");
            System.out.print("Seleccione: ");
            op = sc.nextInt();

            switch (op) {
                case 1:
                    System.out.print("ID: "); int id = sc.nextInt();
                    System.out.print("Nombre: "); String nombre = sc.next();
                    System.out.print("Edad: "); int edad = sc.nextInt();
                    System.out.print("Telefono: "); String tel = sc.next();
                    System.out.print("Historial: "); String hist = sc.next();
                    dao.guardar(new Paciente(id, nombre, edad, tel, hist));
                    System.out.println("Paciente agregado.");
                    break;
                case 2:
                    dao.listarTodos().forEach(p -> System.out.println(p));
                    break;
                case 3:
                    System.out.print("ID: ");
                    Optional<Paciente> p = dao.buscarPorId(sc.nextInt());
                    System.out.println(p.isPresent() ? p.get() : "No encontrado.");
                    break;
                case 4:
                    System.out.print("ID: ");
                    dao.eliminar(sc.nextInt());
                    System.out.println("Eliminado.");
                    break;
            }
        } while (op != 0);
    }
}