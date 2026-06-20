package com.grupo6.subastar.controller;


import com.grupo6.subastar.repository.ClienteRepository;
import com.grupo6.subastar.dto.ActivarRequest;
import com.grupo6.subastar.dto.ClienteDTO;
import com.grupo6.subastar.dto.LoginRequest;
import com.grupo6.subastar.dto.LoginResponse;
import com.grupo6.subastar.dto.RegistroRequest;
import com.grupo6.subastar.model.Cliente;
import com.grupo6.subastar.security.JwtUtils;
import com.grupo6.subastar.security.UserDetailsImpl;
import com.grupo6.subastar.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    




    @Autowired
    private AuthService authService;

    @PostMapping(value = "/registro", consumes = {"multipart/form-data"})
    public ResponseEntity<?> registrar(
            @ModelAttribute RegistroRequest request,
            @RequestParam(value = "fotoDniFrente", required = false) MultipartFile fotoFrente,
            @RequestParam(value = "fotoDniDorso", required = false) MultipartFile fotoDorso) {
        try {
            // Le pasamos los datos y los archivos al servicio
            authService.registrarCliente(request, fotoFrente, fotoDorso);
            return ResponseEntity.ok().body("{\"mensaje\": \"Registro exitoso con fotos\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }


/* 
    // ENDPOINT TEMPORAL PARA GENERAR UN HASH PERFECTO
    @GetMapping("/generar-hash")
    public String generarHash() {
        return passwordEncoder.encode("123456");
    }
    */

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // TELEMETRÍA: Vemos exactamente qué está llegando desde Postman
        System.out.println(">> INTENTO DE LOGIN RECIBIDO:");
        System.out.println(">> Email: " + loginRequest.getEmail());
        System.out.println(">> Clave: " + loginRequest.getClave());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getClave())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generarTokenJwt(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Cliente cliente = userDetails.getCliente();

            ClienteDTO dto = new ClienteDTO();
            dto.setIdentificador(cliente.getIdentificador());
            dto.setNombre(cliente.getPersona().getNombre());
            dto.setApellido(cliente.getPersona().getApellido());
            dto.setEmail(cliente.getPersona().getEmail());
            dto.setCategoria(cliente.getCategoria());
            dto.setAdmitido(cliente.getAdmitido());
            dto.setDocumento(cliente.getPersona().getDocumento());
            dto.setDireccion(cliente.getPersona().getDireccion());
            dto.setPais(cliente.getPais() != null ? cliente.getPais().getNombre() : "Sin país");
            dto.setFoto(cliente.getPersona().getFotoPerfil());

            return ResponseEntity.ok(new LoginResponse(jwt, dto));

        } catch (BadCredentialsException e) {
            System.err.println(">> FALLO: Credenciales incorrectas (Hash no coincide)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email o clave incorrectos.");
        } catch (DisabledException | LockedException e) {
            System.err.println(">> FALLO: Cuenta bloqueada o inactiva");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cuenta pendiente de aprobación, bloqueada o inactiva.");
        } catch (Exception e) {
            System.err.println(">> FALLO: Error interno - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor.");
        }
    }


    @PostMapping("/activar")
    public ResponseEntity<?> activar(@RequestBody ActivarRequest request) {
        try {
            authService.activarCuenta(request);
            
            return ResponseEntity.ok().body("{\"token\": \"token_generado_aqui\", \"mensaje\": \"Cuenta activada. Ya podés iniciar sesión.\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}