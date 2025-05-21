/*Marcos Luque Montiel y Soraya Bennai Sadqi */

package com.uma.example.springuma.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;

import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

public class PacienteControllerMockMvcIT extends AbstractIntegration{
    
    @Autowired
	private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Medico medico;

    private Paciente paciente;

    @BeforeEach
    void setUp() {
        // Crear un médico
        medico = new Medico();
        medico.setId(1);
        medico.setNombre("Dr. Juan");
        medico.setDni("12345678A"); 

        // Crear un paciente
        paciente = new Paciente();
        paciente.setId(1);
        paciente.setNombre("Soraya");
        paciente.setDni("12345678B");
        paciente.setMedico(medico);
    }

//Método privado para guardar un médico    
    private void guardarMedico(Medico medico) throws Exception {
        // Guardar el médico
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());
    }

//Método privado para guardar un paciente
    private void guardarPaciente(Paciente paciente) throws Exception {
        // Guardar el paciente
        this.mockMvc.perform(post("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isCreated());
    }

// En esta prueba se comprueba que el paciente se guarda correctamente y se obtiene correctamente.
    @Test
    @DisplayName("Crear paciente y obtenerlo correctamente")
    void testCrearPacienteYObtenerlo() throws Exception {
        // Guardar el médico
        guardarMedico(medico);
        // Guardar el paciente
        guardarPaciente(paciente);

        // Obtener el paciente
        this.mockMvc.perform(get("/paciente/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Soraya"))
                .andExpect(jsonPath("$.dni").value("12345678B"));
    }

// En esta prueba se comprueba que actualizar el medico de un paciente funciona correctamente    
    @Test
    @DisplayName("Actualizar el médico de un paciente y comprobar el cambio")
    void testActualizarMedicoDePaciente() throws Exception {
        // Guardar el primer médico y el paciente
        guardarMedico(medico);
        guardarPaciente(paciente);

        // Crear y guardar un segundo médico
        Medico medico2 = new Medico();
        medico2.setId(2);
        medico2.setNombre("Dr. Pedro");
        medico2.setDni("87654321C");
        guardarMedico(medico2);

        // Cambiar el médico del paciente
        paciente.setMedico(medico2);

        // Actualizar el paciente
        this.mockMvc.perform(put("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isNoContent());

        // Comprobar que el médico del paciente ha cambiado
        this.mockMvc.perform(get("/paciente/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medico.nombre").value("Dr. Pedro"))
                .andExpect(jsonPath("$.medico.dni").value("87654321C"));
    }

// En esta prueba se comprueba que un paciente borrado no se puede obtener
    @Test
    @DisplayName("Crear paciente, borrarlo y comprobar que no existe")
    void testCrearPacienteYBorrarlo() throws Exception {
        // Guardar el médico
        guardarMedico(medico);
        // Guardar el paciente
        guardarPaciente(paciente);

        // Obtener el paciente
        this.mockMvc.perform(get("/paciente/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Soraya"))
                .andExpect(jsonPath("$.dni").value("12345678B"));

        // Borrar el paciente
        this.mockMvc.perform(delete("/paciente/1"))
                .andExpect(status().isOk());

        // Comprobar que el paciente no existe
        this.mockMvc.perform(get("/paciente/1"))
                .andExpect(status().is5xxServerError());
    }


// En esta prueba se comprueba que el getPacientes de un Medico es correcto
    @Test
    @DisplayName("Crear paciente y comprobar que se obtiene correctamente")
    void testCrearPacienteYObtenerloMedico() throws Exception {
        // Guardar el médico
        guardarMedico(medico);
        // Guardar el paciente
        guardarPaciente(paciente);

        // Creamos y guardamos un segundo paciente
        Paciente paciente2 = new Paciente();
        paciente2.setId(2);
        paciente2.setNombre("Marcos");
        paciente2.setDni("12345678C");
        paciente2.setMedico(medico);
        guardarPaciente(paciente2);

        // Obtener los pacientes del médico
        this.mockMvc.perform(get("/paciente/medico/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Soraya"))
                .andExpect(jsonPath("$[0].dni").value("12345678B"))
                .andExpect(jsonPath("$[1].nombre").value("Marcos"))
                .andExpect(jsonPath("$[1].dni").value("12345678C"));
    }

// En esta prueba comprobamos que no se puede obtener un paciente que no existe
    @Test
    @DisplayName("Comprobar que no se puede obtener un paciente que no existe")
    void testObtenerPacienteQueNoExiste() throws Exception {
        // Intentar obtener un paciente que no existe
        this.mockMvc.perform(get("/paciente/1"))
                .andExpect(status().is5xxServerError());
    }

// En esta prueba comprobamos que no se puede editar un paciente con el dni de uno ya existente
    @Test
    @DisplayName("Actualizar paciente con dni existente da error")
    void testActualizarPacienteConDniExistente() throws Exception {
        // Guardar el médico
        guardarMedico(medico);
        // Guardar el paciente
        guardarPaciente(paciente);

        // Crear otro paciente con un dni diferente
        Paciente paciente2 = new Paciente();
        paciente2.setId(2);
        paciente2.setNombre("Marcos");
        paciente2.setDni("12345678C");
        paciente2.setMedico(medico);
        guardarPaciente(paciente2);

        // Comprobar que se guarda correctamente
        this.mockMvc.perform(get("/paciente/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Marcos"))
                .andExpect(jsonPath("$.dni").value("12345678C"));

        // Modificar el paciente con el mismo dni que el primero
        paciente2.setDni("12345678B");

        // Comprobar que no se puede actualizar el paciente
        this.mockMvc.perform(put("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente2)))
                .andExpect(status().is5xxServerError());
    }

// En esta prueba comprobamos que no se puede borrar un paciente que no existe
    @Test
    @DisplayName("Borrar paciente que no existe da error")
    void testBorrarPacienteQueNoExiste() throws Exception {
        // Comprobar que no se puede borrar el paciente
        this.mockMvc.perform(delete("/paciente/1"))
                .andExpect(status().is5xxServerError());
    }

// En esta prueba comprobamos que no se puede crear un paciente con el dni de otro que ya existe
    @Test
    @DisplayName("Crear paciente con dni existente da error")
    void testCrearPacienteConDniExistente() throws Exception {
        // Guardar el médico
        guardarMedico(medico);
        // Guardar el paciente
        guardarPaciente(paciente);

        // Crear otro paciente con el mismo dni
        Paciente paciente2 = new Paciente();
        paciente2.setNombre("Marcos");
        paciente2.setDni("12345678B");
        paciente2.setMedico(medico);

        // Comprobar que no se puede crear el paciente
        this.mockMvc.perform(post("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente2)))
                .andExpect(status().is5xxServerError());
    }

// En esta prueba comprobamos que el get Paciente de un medico que no tienes pacientes devuelve una lista vacía
    @Test
    @DisplayName("Comprobar que el getPacientes de un médico sin pacientes devuelve una lista vacía")
    void testGetPacientesDeMedicoSinPacientes() throws Exception {
        // Guardar el médico
        guardarMedico(medico);

        // Obtener los pacientes del médico
        this.mockMvc.perform(get("/paciente/medico/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

}

