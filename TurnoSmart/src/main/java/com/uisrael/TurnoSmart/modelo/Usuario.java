package com.uisrael.TurnoSmart.modelo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
public class Usuario implements UserDetails, Serializable{/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idUsuario;
    private String username;
    private String password;
    
    @ManyToOne
    @JoinColumn(name = "id_docente")
    @ToString.Exclude
    private Docente docente;

    @ManyToOne
    @JoinColumn(name = "id_representante")
    @ToString.Exclude
    private Representante representante;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuarios_roles",
        joinColumns = @JoinColumn(name = "id_usuario"),
        inverseJoinColumns = @JoinColumn(name = "id_role")
    )
    @ToString.Exclude
    private List<Role> roles;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                    .map(role -> (GrantedAuthority) () -> role.getNombre())
                    .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Cambia esto según tu lógica
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Cambia esto según tu lógica
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Cambia esto según tu lógica
    }

    @Override
    public boolean isEnabled() {
        return true; // Cambia esto según tu lógica
    }
}
