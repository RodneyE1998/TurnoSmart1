package com.uisrael.TurnoSmart.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uisrael.TurnoSmart.modelo.Docente;

public interface DocenteRepositorio extends JpaRepository<Docente, Integer> {
	// Aquí podemos agregar métodos personalizados para realizar operaciones específicas
	
	Docente findByNumeroIdentificacion(String numeroIdentificacion);
    
    void deleteByNumeroIdentificacion(String numeroIdentificacion);

}