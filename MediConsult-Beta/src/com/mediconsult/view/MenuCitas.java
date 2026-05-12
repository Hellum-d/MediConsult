/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mediconsult.view;

import com.mediconsult.dao.impl.CitaDAOImpl;
import com.mediconsult.model.Cita;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Scanner;

public class MenuCitas {
    private CitaDAOImpl dao = new CitaDAOImpl();
    private Scanner sc = new Scanner(System.in);

    public void mostrar() {
        int op;
        do {
            System.out.println("\n--- Menu Citas ---");
            System.out.println("1. Agregar cita");
            System.out.println("2. Listar citas");
            System.out.println("3. Buscar cita");
            System.out.println("4. Eliminar cita");
            System.out.println("0. Volver");
            System.out.print("Seleccione: ");
            op = sc.nextInt();

            switch (op) {
                case 1:
                    System.out.print("ID: "); int id = sc.nextInt();
                    System.out.print("ID Paciente: "); int pacId = sc.nextInt();
                    System.out.print("ID Medico: "); int medId = sc.nextInt();
                    System.out.print("Fecha (YYYY-MM-DD): "); String fecha = sc.next();
                    System.out.print("Estado (PENDIENTE/COMPLETADA/CANCELADA): "); String estado = sc.next();
                    dao.guardar(new Cita(id, pacId, medId, LocalDate.parse(fecha), estado));
                    System.out.println("Cita registrada.");
                    break;
                case 2:
                    dao.listarTodas().forEach(c -> System.out.println(c));
                    break;
                case 3:
                    System.out.print("ID: ");
                    Optional<Cita> c = dao.buscarPorId(sc.nextInt());
                    System.out.println(c.isPresent() ? c.get() : "No encontrada.");
                    break;
                case 4:
                    System.out.print("ID: ");
                    dao.eliminar(sc.nextInt());
                    System.out.println("Eliminada.");
                    break;
            }
        } while (op != 0);
    }
}