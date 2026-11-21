/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mediconsult.view;

import com.mediconsult.dao.impl.MedicoDAOImpl;
import com.mediconsult.model.Medico;
import java.util.Optional;
import java.util.Scanner;

public class MenuMedicos {
    private MedicoDAOImpl dao = new MedicoDAOImpl();
    private Scanner sc = new Scanner(System.in);

    public void mostrar() {
        int op;
        do {
            System.out.println("\n--- Menu Medicos ---");
            System.out.println("1. Agregar medico");
            System.out.println("2. Listar medicos");
            System.out.println("3. Buscar medico");
            System.out.println("4. Eliminar medico");
            System.out.println("0. Volver");
            System.out.print("Seleccione: ");
            op = sc.nextInt();

            switch (op) {
                case 1:
                    System.out.print("ID: "); int id = sc.nextInt();
                    System.out.print("Nombre: "); String nombre = sc.next();
                    System.out.print("Especialidad: "); String esp = sc.next();
                    System.out.print("Disponible (true/false): "); boolean disp = sc.nextBoolean();
                    dao.guardar(new Medico(id, nombre, esp, disp));
                    System.out.println("Medico agregado.");
                    break;
                case 2:
                    dao.listarTodos().forEach(m -> System.out.println(m));
                    break;
                case 3:
                    System.out.print("ID: ");
                    Optional<Medico> m = dao.buscarPorId(sc.nextInt());
                    System.out.println(m.isPresent() ? m.get() : "No encontrado.");
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