package com.uisrael.TurnoSmart.controlador;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.uisrael.TurnoSmart.modelo.Cita;
import com.uisrael.TurnoSmart.modelo.Docente;
import com.uisrael.TurnoSmart.modelo.Estudiante;
import com.uisrael.TurnoSmart.modelo.Representante;
import com.uisrael.TurnoSmart.modelo.Usuario;
import com.uisrael.TurnoSmart.repositorio.UsuarioRepositorio;
import com.uisrael.TurnoSmart.servicio.CitaServicio;
import com.uisrael.TurnoSmart.servicio.DocenteServicio;
import com.uisrael.TurnoSmart.servicio.EmailServicio;
import com.uisrael.TurnoSmart.servicio.EstudianteServicio;

@Controller
@RequestMapping("/docente")
public class DocenteControlador {

	private final UsuarioRepositorio usuarioRepositorio;
	private final CitaServicio citaServicio;
	private final EstudianteServicio estudianteServicio;
	private final EmailServicio emailServicio;
	private final DocenteServicio docenteServicio;

	public DocenteControlador(UsuarioRepositorio usuarioRepositorio, CitaServicio citaServicio,
			EstudianteServicio estudianteServicio, EmailServicio emailServicio, DocenteServicio docenteServicio) {
		this.usuarioRepositorio = usuarioRepositorio;
		this.citaServicio = citaServicio;
		this.estudianteServicio = estudianteServicio;
		this.emailServicio = emailServicio;
		this.docenteServicio = docenteServicio;
	}

	@GetMapping("/PrincipalDocente")
	public String mostrarPaginaDocente() {
		return "PrincipalDocente";
	}

	@GetMapping("/Perfil")
	public String mostrarPerfilDocente(Principal principal, Model model) {
	    // Obtener el usuario autenticado
	    String username = principal.getName();
	    Usuario usuario = usuarioRepositorio.findByUsername(username)
	            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

	    // Obtener el docente asociado al usuario
	    Docente docente = usuario.getDocente();
	    if (docente == null) {
	        throw new RuntimeException("El usuario no tiene un docente asociado.");
	    }

	    // Pasar los datos del docente al modelo
	    model.addAttribute("docente", docente);
	    return "PerfilDocente"; 
	}


	@GetMapping("/citas")
	public String mostrarPaginaCitas(Model model, Principal principal) {
		// Obtener el ID del docente desde el usuario autenticado
		String username = principal.getName();
		Usuario usuario = usuarioRepositorio.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

		Docente docente = usuario.getDocente();
		if (docente == null) {
			throw new RuntimeException("El usuario no tiene un docente asociado");
		}

		// Obtener los estudiantes del docente
		List<Estudiante> estudiantes = estudianteServicio.obtenerEstudiantesPorDocente(docente.getIdDocente());

		// Pasar los estudiantes al modelo
		model.addAttribute("estudiantes", estudiantes);
		return "CitasDocentes";
	}

	@PostMapping("/citas/agendar")
	public String agendarCitaPorDocente(@ModelAttribute Cita cita, @RequestParam("idEstudiante") Integer idEstudiante, Principal principal) {
	    try {
	        // Obtener usuario autenticado
	        String username = principal.getName();
	        Usuario usuario = usuarioRepositorio.findByUsername(username)
	                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

	        Docente docente = usuario.getDocente();
	        if (docente == null) {
	            throw new RuntimeException("El usuario no tiene un docente asociado");
	        }

	        // Agendar la cita
	        citaServicio.agendarCitaPorDocente(cita, idEstudiante, docente.getIdDocente());

	        // Obtener datos para enviar el correo
	        Representante representante = cita.getRepresentante();
	        String destinatario = representante.getEmail();
	        String nombreRepresentante = representante.getNombre();
	        String nombreDocente = docente.getNombre() + " " + docente.getApellido();
	        String fecha = cita.getFechaCita().toString();
	        String hora = cita.getHoraCita().toString();
	        Integer idCita = cita.getIdCita();

	        // Enviar correo con botón de confirmación
	        emailServicio.enviarCorreoConfirmacionCita(destinatario, nombreRepresentante, nombreDocente, fecha, hora, idCita);

	        return "redirect:/docente/citas/agendadas";

	    } catch (Exception e) {
	        e.printStackTrace();
	        return "redirect:/docente/citas/agendadas?error";
	    }
	}


	@GetMapping("/citas/agendadas")
	public String mostrarCitasAgendadas(Model model, Principal principal) {
		// Obtener el docente autenticado
		String username = principal.getName();
		Docente docente = usuarioRepositorio.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Usuario no encontrado")).getDocente();

		// Obtener las citas del docente
		List<Cita> citas = citaServicio.obtenerCitasPorDocente(docente.getIdDocente());

		// Clasificar las citas por tipo
		List<Cita> citasConEstudiante = citas.stream().filter(cita -> cita.getEstudiante() != null).toList();

		List<Cita> citasSinEstudiante = citas.stream().filter(cita -> cita.getEstudiante() == null).toList();

		// Agregar la lista de estudiantes relacionados a cada representante en las
		// citas sin estudiante
		for (Cita cita : citasSinEstudiante) {
			Representante representante = cita.getRepresentante();
			if (representante != null) {
				// Cargar los estudiantes relacionados al representante
				representante.setEstudiantes(
						estudianteServicio.obtenerEstudiantesPorRepresentante(representante.getIdRepresentante()));
			}
		}

		// Pasar las citas al modelo
		model.addAttribute("citasConEstudiante", citasConEstudiante);
		model.addAttribute("citasSinEstudiante", citasSinEstudiante);

		return "CitasAgendadasDocente";
	}

	@PostMapping("/modificar-cita")
	@ResponseBody
	public String modificarCita(@RequestBody Map<String, String> datos) {
		try {
			Integer idCita = Integer.parseInt(datos.get("idCita"));
			String nuevaFecha = datos.get("nuevaFecha");
			String nuevaHora = datos.get("nuevaHora");
			citaServicio.modificarCita(idCita, LocalDate.parse(nuevaFecha), LocalTime.parse(nuevaHora));
			return "Cita modificada exitosamente.";
		} catch (Exception e) {
			return "Error al modificar la cita: " + e.getMessage();
		}
	}

	@PostMapping("/guardar-cita")
	@ResponseBody
	public String guardarCita(@RequestBody Map<String, Object> datos) {
	    try {
	        // Obtener datos enviados desde el frontend
	        System.out.println("Datos recibidos: " + datos);
	        Integer idCita = (Integer) datos.get("idCita");
	        String nuevaFecha = (String) datos.get("nuevaFecha");
	        String nuevaHora = (String) datos.get("nuevaHora");

	        // Actualizar la cita en la base de datos
	        citaServicio.modificarCita(idCita, LocalDate.parse(nuevaFecha), LocalTime.parse(nuevaHora));
	        System.out.println("Cita actualizada en la base de datos.");

	        // Obtener la cita actualizada para enviar el correo
	        Cita cita = citaServicio.obtenerCitaPorId(idCita);

	        // Validar si tiene docentes asociados
	        if (cita.getDocentes() == null || cita.getDocentes().isEmpty()) {
	            throw new RuntimeException("La cita no tiene docentes asociados.");
	        }

	        // Obtener datos necesarios para el correo
	        String destinatario = cita.getRepresentante().getEmail(); // Correo del representante
	        String asunto = "Cambio en la fecha de su cita";
	        String mensaje = "Estimado/a " + cita.getRepresentante().getNombre() + ",\n\n" +
	                "El/La docente " + cita.getDocentes().get(0).getNombre() + " " + cita.getDocentes().get(0).getApellido() +
	                " ha cambiado la fecha y hora de su cita.\n\n" +
	                "Nueva fecha: " + nuevaFecha + "\n" +
	                "Nueva hora: " + nuevaHora + "\n\n" +
	                "Por favor, revise su disponibilidad caso contrario ingrese al sisteme y cancele la cita.\n\n" +
	                "Atentamente,\n" +
	                "Colegio Antonio Flores";

	        // Enviar correo
	        System.out.println("Preparando envío de correo...");
	        emailServicio.enviarCorreo(destinatario, asunto, mensaje);
	        System.out.println("Correo enviado exitosamente.");

	        // Responder al cliente con mensaje de éxito
	        return "Cita actualizada y correo enviado correctamente.";
	    } catch (Exception e) {
	        // Manejar errores y responder al cliente
	        System.err.println("Error al actualizar la cita: " + e.getMessage());
	        return "Error al actualizar la cita: " + e.getMessage();
	    }
	}


	@PostMapping("/cancelar-cita")
	@ResponseBody
	public String cancelarCita(@RequestParam("idCita") Integer idCita) {
		try {
			citaServicio.cancelarCita(idCita);

			// Obtener los datos de la cita para el correo
			Cita cita = citaServicio.obtenerCitaPorId(idCita); // Usar el nuevo método
			String destinatario = cita.getRepresentante().getEmail();
			String asunto = "Cita Cancelada CEIAF";
			String mensaje = "La cita con el/la docente " + cita.getDocentes().get(0).getNombre() + " "
					+ cita.getDocentes().get(0).getApellido() + " para la fecha " + cita.getFechaCita() + " a las "
					+ cita.getHoraCita() + " ha sido cancelada.";

			// Enviar correo
			emailServicio.enviarCorreo(destinatario, asunto, mensaje);

			return "Cita cancelada y correo enviado al representante.";
		} catch (Exception e) {
			return "Error al cancelar la cita: " + e.getMessage();
		}
	}

	@PostMapping("/confirmar-cita")
	@ResponseBody
	public String confirmarCita(@RequestParam("idCita") Integer idCita) {
		try {
			citaServicio.confirmarCita(idCita);

			// Obtener los datos de la cita para el correo
			Cita cita = citaServicio.obtenerCitaPorId(idCita); // Usar el nuevo método
			String destinatario = cita.getRepresentante().getEmail();
			String asunto = "Cita Confirmada";
			String mensaje = "Su cita con el docente " + cita.getDocentes().get(0).getNombre() + " "
					+ cita.getDocentes().get(0).getApellido() + " para la fecha " + cita.getFechaCita() + " a las "
					+ cita.getHoraCita() + " ha sido confirmada.";

			// Enviar correo
			emailServicio.enviarCorreo(destinatario, asunto, mensaje);

			return "Cita confirmada y correo enviado al representante.";
		} catch (Exception e) {
			return "Error al confirmar la cita: " + e.getMessage();
		}
	}
	
	@PostMapping("/citas/realizar")
	@ResponseBody
	public String marcarComoRealizada(@RequestParam("idCita") Integer idCita) {
	    try {
	        // Cambiar el estado de la cita a "REALIZADA"
	        citaServicio.marcarComoRealizada(idCita);

	        // Obtener la cita actualizada para enviar el correo
	        Cita cita = citaServicio.obtenerCitaPorId(idCita);

	        // Validar que la cita tenga un representante asociado
	        if (cita.getRepresentante() == null) {
	            throw new RuntimeException("No se encontró un representante asociado a esta cita.");
	        }

	        // Preparar los datos del correo
	        String destinatario = cita.getRepresentante().getEmail(); // Correo del representante
	        String asunto = "Confirmación de cita realizada";
	        String mensaje = "Estimado/a " + cita.getRepresentante().getNombre() + ",\n\n" +
	                "Le informamos que la cita realizada el " +
	                cita.getFechaCita() + " a las " + cita.getHoraCita() + " ha sido completada con éxito.\n\n" +
	                "Gracias por utilizar nuestra aplicación para agendar y gestionar sus citas.\n" +
	                "No olvide seguir utilizando nuestro sistema para programar futuras reuniones.\n\n" +
	                "Atentamente,\n" +
	                "Colegio Antonio Flores";

	        // Enviar correo electrónico
	        emailServicio.enviarCorreo(destinatario, asunto, mensaje);

	        // Retornar mensaje de éxito
	        return "Cita marcada como realizada y correo enviado correctamente.";
	    } catch (Exception e) {
	        return "Error al marcar la cita como realizada: " + e.getMessage();
	    }
	}
	
	/*@GetMapping("/disponibles/{idRepresentante}")
	@ResponseBody
	public List<Docente> obtenerDocentesDisponibles(@PathVariable Integer idRepresentante) {
	    return docenteServicio.obtenerDocentesPorRepresentante(idRepresentante);
	}*/




}
