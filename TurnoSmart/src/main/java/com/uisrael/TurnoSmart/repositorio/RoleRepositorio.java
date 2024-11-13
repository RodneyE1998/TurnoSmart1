package com.uisrael.TurnoSmart.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uisrael.TurnoSmart.modelo.Role;


public interface RoleRepositorio extends JpaRepository<Role, Integer> {
	
	Role findByNombre(String nombre);

}