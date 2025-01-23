package com.uisrael.TurnoSmart.servicio.impl;


import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uisrael.TurnoSmart.modelo.Cita;
import com.uisrael.TurnoSmart.modelo.Docente;
import com.uisrael.TurnoSmart.modelo.Estudiante;
import com.uisrael.TurnoSmart.modelo.HorarioDisponible;
import com.uisrael.TurnoSmart.modelo.Representante;
import com.uisrael.TurnoSmart.repositorio.CitaRepositorio;
import com.uisrael.TurnoSmart.repositorio.DocenteRepositorio;
import com.uisrael.TurnoSmart.repositorio.EstudianteRepositorio;
import com.uisrael.TurnoSmart.repositorio.HorarioDisponibleRepositorio;
import com.uisrael.TurnoSmart.servicio.CitaServicio;

@Service
@Transactional
public class CitaServicioImpl implements CitaServicio {

	@Autowired
	private CitaRepositorio citaRepositorio;

	@Autowired
	private EstudianteRepositorio estudianteRepositorio;

	@Autowired
	private DocenteRepositorio docenteRepositorio;

	@Autowired
	private HorarioDisponibleRepositorio horarioRepositorio;

	@Override
	public List<Cita> getAllCitas() {
		// TODO Auto-generated method stub
		return citaRepositorio.findAll();
	}

	@Override
	public Cita getCitaById(Integer id) {
		// TODO Auto-generated method stub
		return citaRepositorio.findById(id).orElseThrow();
	}

	@Override
	public Cita createCita(Cita cita) {
		// TODO Auto-generated method stub
		return citaRepositorio.save(cita);
	}

	@Override
	public Cita updateCita(Cita cita) {
		// TODO Auto-generated method stub
		return citaRepositorio.save(cita);
	}

	@Override
	public void deleteCita(Integer id) {
		// TODO Auto-generated method stub
		citaRepositorio.deleteById(id);
	}

	@Override
	@Transactional
	public void agendarCitaPorDocente(Cita cita, Integer idEstudiante, Integer idDocente) {

		// Obtener el estudiante
		Estudiante estudiante = estudianteRepositorio.findById(idEstudiante)
				.orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

		// Obtener el representante del estudiante
		Representante representante = estudiante.getRepresentante();

		// Obtener el docente
		Docente docente = docenteRepositorio.findById(idDocente)
				.orElseThrow(() -> new RuntimeException("Docente no encontrado"));

		// Configurar la cita
		cita.setEstudiante(estudiante);
		cita.setRepresentante(representante);
		cita.setDocentes(List.of(docente));
		cita.setEstadoCita("PENDIENTE");

		// Guardar la cita
		citaRepositorio.save(cita);

	}

	@Override
	public List<Cita> obtenerCitasPorDocente(Integer idDocente) {
		return citaRepositorio.findByDocentesIdDocente(idDocente);
	}

	@Override
	@Transactional
	public void agendarCitaPorRepresentante(Integer idDocente, Integer idHorario, Representante representante,
			LocalDate fecha) {
		// Buscar el docente
		Docente docente = docenteRepositorio.findById(idDocente)
				.orElseThrow(() -> new RuntimeException("Docente no encontrado con el ID: " + idDocente));

		// Buscar el horario
		HorarioDisponible horario = horarioRepositorio.findById(idHorario)
				.orElseThrow(() -> new RuntimeException("Horario no encontrado con el ID: " + idHorario));

		// Validar si ya existe una cita para el horario seleccionado
		boolean citaExistente = citaRepositorio.existsByFechaCitaAndHoraCitaAndDocentes(fecha, horario.getHoraInicio(),
				docente);
		if (citaExistente) {
			throw new RuntimeException("El horario seleccionado ya está reservado para este docente.");
		}

		// Crear la nueva cita
		Cita nuevaCita = new Cita();
		nuevaCita.setDocentes(List.of(docente));
		nuevaCita.setEstadoCita("PENDIENTE");
		nuevaCita.setFechaCita(fecha); // Usar la fecha seleccionada por el usuario
		nuevaCita.setHoraCita(horario.getHoraInicio());
		nuevaCita.setRepresentante(representante);

		// Guardar la cita en el repositorio
		citaRepositorio.save(nuevaCita);
	}

	@Override
	public List<Cita> obtenerCitasPorRepresentante(Integer idRepresentante) {
		// TODO Auto-generated method stub
		return citaRepositorio.findByRepresentanteIdRepresentante(idRepresentante);
	}

}
