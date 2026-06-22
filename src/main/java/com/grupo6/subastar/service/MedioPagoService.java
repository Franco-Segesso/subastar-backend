package com.grupo6.subastar.service;

import com.grupo6.subastar.dto.*;
import com.grupo6.subastar.model.*;
import com.grupo6.subastar.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Service
public class MedioPagoService {

    @Autowired
    private MedioPagoRepository medioPagoRepository;

    @Autowired
    private TarjetaCreditoRepository tarjetaRepository;

    @Autowired
    private CuentaBancariaRepository cuentaRepository;

    @Autowired
    private ChequeCertificadoRepository chequeRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private RegistroSubastaRepository registroSubastaRepository;

    // GET: traer todos los medios de pago activos de un cliente
    public List<MedioPago> obtenerMediosPago(Integer clienteId) throws Exception {
        clienteRepository.findById(clienteId)
                .orElseThrow(() -> new Exception("Cliente no encontrado con id: " + clienteId));
        return completarDisponibilidad(
                medioPagoRepository.findByClienteIdentificadorAndActivo(clienteId, "si"));
    }

    public List<MedioPago> obtenerMediosPago(String email) {
        Cliente cliente = clienteRepository.findByPersonaEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "401: Token invalido, ausente o expirado"));
        return completarDisponibilidad(
                medioPagoRepository.findByClienteIdentificadorAndActivo(
                        cliente.getIdentificador(),
                        "si"));
    }

    @Transactional(readOnly = true)
    public void validarDisponibilidadParaSubasta(
            Cliente cliente,
            String monedaSubasta) {
        String moneda = monedaSubasta == null
                ? "" : monedaSubasta.trim().toUpperCase(Locale.ROOT);
        List<MedioPago> activos = medioPagoRepository
                .findByClienteIdentificadorAndActivo(
                        cliente.getIdentificador(), "si");

        boolean compatible = activos.stream().anyMatch(
                medio -> esCompatibleSinValidarSaldo(medio, moneda));
        if (!compatible) {
            throw new RuntimeException(
                    "403: Necesitas un medio de pago vigente y compatible con la moneda de la subasta");
        }
    }

    // POST: agregar tarjeta
    @Transactional
    public MedioPago agregarTarjeta(Integer clienteId, AgregarTarjetaRequest req) throws Exception {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new Exception("Cliente no encontrado con id: " + clienteId));

        
        List<MedioPago> activos = medioPagoRepository.findByClienteIdentificadorAndActivo(clienteId, "si");
        for (MedioPago m : activos) {
            if (m instanceof TarjetaCredito) {
                TarjetaCredito tc = (TarjetaCredito) m;
                boolean mismosDigitos = tc.getUltimosDigitos().equals(req.getUltimosDigitos());
                boolean mismoVencimiento = tc.getVencimiento().equals(req.getVencimiento());
                boolean mismoTitular = tc.getTitular().equalsIgnoreCase(req.getTitular());
                
                if (mismosDigitos && mismoVencimiento && mismoTitular) {
                    throw new Exception("Esta tarjeta de crédito ya se encuentra registrada en tu cuenta.");
                }
            }
        }

        TarjetaCredito tarjeta = new TarjetaCredito();
        tarjeta.setCliente(cliente);
        tarjeta.setActivo("si");
        tarjeta.setUltimosDigitos(req.getUltimosDigitos());
        tarjeta.setVencimiento(req.getVencimiento());
        tarjeta.setTitular(req.getTitular());
        tarjeta.setEsExtranjera(req.getEsExtranjera());
        tarjeta.setPaisEmisor(req.getPaisEmisor());

        TarjetaCredito guardada = tarjetaRepository.save(tarjeta);
        evaluarYActualizarCategoria(clienteId);
        return guardada;
    }

    // POST: agregar cuenta bancaria
    @Transactional
    public MedioPago agregarCuenta(Integer clienteId, AgregarCuentaRequest req) throws Exception {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new Exception("Cliente no encontrado con id: " + clienteId));


        List<MedioPago> activos = medioPagoRepository.findByClienteIdentificadorAndActivo(clienteId, "si");
        for (MedioPago m : activos) {
            if (m instanceof CuentaBancaria) {
                CuentaBancaria cb = (CuentaBancaria) m;
                if (cb.getCbuIban().equals(req.getCbuIban())) {
                    throw new Exception("Ya tenés una cuenta registrada con el CBU/IBAN: " + req.getCbuIban());
                }
            }
        }

        CuentaBancaria cuenta = new CuentaBancaria();
        cuenta.setCliente(cliente);
        cuenta.setActivo("si");
        cuenta.setCbuIban(req.getCbuIban());
        cuenta.setAlias(req.getAlias());
        cuenta.setBanco(req.getBanco());
        cuenta.setPaisBanco(req.getPaisBanco());
        cuenta.setFondosReservados(req.getFondosReservados());
        cuenta.setMoneda(req.getMoneda());

        CuentaBancaria guardada = cuentaRepository.save(cuenta);
        evaluarYActualizarCategoria(clienteId);
        return guardada;
    }

    // POST: agregar cheque
    @Transactional
    public MedioPago agregarCheque(Integer clienteId, AgregarChequeRequest req) throws Exception {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new Exception("Cliente no encontrado con id: " + clienteId));


        List<MedioPago> activos = medioPagoRepository.findByClienteIdentificadorAndActivo(clienteId, "si");
        for (MedioPago m : activos) {
            if (m instanceof ChequeCertificado) {
                ChequeCertificado cc = (ChequeCertificado) m;
                if (cc.getNroCheque().equals(req.getNroCheque())) {
                    throw new Exception("Ya tenés registrado un cheque con el número: " + req.getNroCheque());
                }
            }
        }

        ChequeCertificado cheque = new ChequeCertificado();
        cheque.setCliente(cliente);
        cheque.setActivo("si");
        cheque.setVerificadoCheque("no");
        cheque.setNroCheque(req.getNroCheque());
        cheque.setBanco(req.getBanco());
        cheque.setMoneda(req.getMoneda());
        cheque.setMontoGarantia(req.getMontoGarantia());
        cheque.setFechaEntrega(req.getFechaEntrega());

        ChequeCertificado guardado = chequeRepository.save(cheque);
        evaluarYActualizarCategoria(clienteId);
        return guardado;
    }

    // DELETE
    @Transactional
    public void darDeBaja(Integer medioPagoId) throws Exception {
        MedioPago medio = medioPagoRepository.findById(medioPagoId)
                .orElseThrow(() -> new Exception("Medio de pago no encontrado con id: " + medioPagoId));
        medio.setActivo("no");
        medioPagoRepository.save(medio);
    }

    @Transactional(readOnly = true)
    public MedioPago validarParaPuja(
            Integer medioPagoId,
            Cliente cliente,
            String monedaSubasta) {
        MedioPago medio = obtenerMedioDelCliente(medioPagoId, cliente);
        validarActivoYMoneda(medio, monedaSubasta);
        return medio;
    }

    @Transactional(readOnly = true)
    public boolean puedeCubrirTotal(
            Integer medioPagoId,
            Cliente cliente,
            String monedaCompra,
            Double total) {
        MedioPago medio = obtenerMedioDelCliente(medioPagoId, cliente);
        validarActivoYMoneda(medio, monedaCompra);
        BigDecimal importe = BigDecimal.valueOf(total == null ? 0.0 : total);

        if (medio instanceof CuentaBancaria cuenta) {
            return saldo(cuenta.getFondosReservados()).compareTo(importe) >= 0;
        }
        if (medio instanceof ChequeCertificado cheque) {
            return "si".equalsIgnoreCase(cheque.getVerificadoCheque())
                    && saldo(cheque.getMontoGarantia()).compareTo(importe) >= 0;
        }
        return true;
    }

    @Transactional
    public MedioPago cobrar(
            Integer medioPagoId,
            Cliente cliente,
            String monedaCompra,
            Double total) {
        MedioPago medio = obtenerMedioDelCliente(medioPagoId, cliente);
        validarActivoYMoneda(medio, monedaCompra);
        BigDecimal importe = BigDecimal.valueOf(total == null ? 0.0 : total);

        if (medio instanceof CuentaBancaria cuenta) {
            BigDecimal disponible = saldo(cuenta.getFondosReservados());
            if (disponible.compareTo(importe) < 0) {
                throw new RuntimeException("400: Fondos reservados insuficientes");
            }
            cuenta.setFondosReservados(disponible.subtract(importe));
            cuentaRepository.save(cuenta);
        } else if (medio instanceof ChequeCertificado cheque) {
            if (!"si".equalsIgnoreCase(cheque.getVerificadoCheque())) {
                throw new RuntimeException("400: El cheque no esta verificado");
            }
            BigDecimal disponible = saldo(cheque.getMontoGarantia());
            if (disponible.compareTo(importe) < 0) {
                throw new RuntimeException("400: Garantia del cheque insuficiente");
            }
            cheque.setMontoGarantia(disponible.subtract(importe));
            chequeRepository.save(cheque);
        }
        return medio;
    }

    private MedioPago obtenerMedioDelCliente(Integer medioPagoId, Cliente cliente) {
        if (medioPagoId == null) {
            throw new RuntimeException("400: Debe seleccionar un medio de pago");
        }
        MedioPago medio = medioPagoRepository.findById(medioPagoId)
                .orElseThrow(() -> new RuntimeException("404: Medio de pago inexistente"));
        if (medio.getCliente() == null || cliente == null
                || !cliente.getIdentificador().equals(
                medio.getCliente().getIdentificador())) {
            throw new RuntimeException("403: El medio de pago no pertenece al cliente");
        }
        return medio;
    }

    private void validarActivoYMoneda(MedioPago medio, String moneda) {
        if (!"si".equalsIgnoreCase(medio.getActivo())) {
            throw new RuntimeException("403: El medio de pago esta inactivo");
        }
        String monedaNormalizada = moneda == null
                ? "" : moneda.trim().toUpperCase(Locale.ROOT);

        if (medio instanceof CuentaBancaria cuenta
                && !monedaNormalizada.equalsIgnoreCase(cuenta.getMoneda())) {
            throw new RuntimeException("403: La moneda de la cuenta no coincide con la subasta");
        }
        if (medio instanceof ChequeCertificado cheque
                && !monedaNormalizada.equalsIgnoreCase(cheque.getMoneda())) {
            throw new RuntimeException("403: La moneda del cheque no coincide con la subasta");
        }
        if (medio instanceof TarjetaCredito tarjeta) {
            validarVencimiento(tarjeta.getVencimiento());
            if ("USD".equals(monedaNormalizada)
                    && !"si".equalsIgnoreCase(tarjeta.getEsExtranjera())) {
                throw new RuntimeException("403: Para compras en USD se requiere una tarjeta internacional");
            }
        }
    }

    private boolean esCompatibleSinValidarSaldo(
            MedioPago medio,
            String moneda) {
        try {
            validarActivoYMoneda(medio, moneda);
            if (medio instanceof ChequeCertificado cheque) {
                return "si".equalsIgnoreCase(cheque.getVerificadoCheque());
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void validarVencimiento(String vencimiento) {
        try {
            YearMonth fecha = YearMonth.parse(
                    vencimiento,
                    DateTimeFormatter.ofPattern("MM/yy"));
            if (fecha.isBefore(YearMonth.now())) {
                throw new RuntimeException("403: La tarjeta esta vencida");
            }
        } catch (DateTimeParseException e) {
            throw new RuntimeException("403: Vencimiento de tarjeta invalido");
        }
    }

    private BigDecimal saldo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private List<MedioPago> completarDisponibilidad(List<MedioPago> medios) {
        for (MedioPago medio : medios) {
            BigDecimal comprometido = BigDecimal.valueOf(valor(
                    registroSubastaRepository.sumPendienteByMedioPagoId(
                            medio.getIdentificador())));
            if (medio instanceof CuentaBancaria cuenta) {
                cuenta.setFondosDisponibles(
                        saldo(cuenta.getFondosReservados())
                                .subtract(comprometido)
                                .max(BigDecimal.ZERO));
            } else if (medio instanceof ChequeCertificado cheque) {
                cheque.setFondosDisponibles(
                        saldo(cheque.getMontoGarantia())
                                .subtract(comprometido)
                                .max(BigDecimal.ZERO));
            }
        }
        return medios;
    }

    private double valor(Double numero) {
        return numero == null ? 0.0 : numero;
    }

    @Transactional
    public void evaluarYActualizarCategoria(Integer clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) return;

        long cantidadMedios = medioPagoRepository.findByClienteIdentificadorAndActivo(clienteId, "si").size();
        long cantidadCompras = registroSubastaRepository.countByClienteId(clienteId);

        String categoriaActual = cliente.getCategoria() != null ? cliente.getCategoria().toLowerCase() : "comun";
        int pesoActual = obtenerPesoCategoria(categoriaActual);
        
        String nuevaCategoria = categoriaActual;
        int nuevoPeso = pesoActual;

        // Reglas de negocio para ascensos (solo suben, nunca bajan)
        if (cantidadCompras >= 10 && cantidadMedios >= 5) {
            if (nuevoPeso < 5) { nuevaCategoria = "platino"; nuevoPeso = 5; }
        } else if (cantidadCompras >= 5 && cantidadMedios >= 3) {
            if (nuevoPeso < 4) { nuevaCategoria = "oro"; nuevoPeso = 4; }
        } else if (cantidadCompras >= 2 || cantidadMedios >= 3) {
            if (nuevoPeso < 3) { nuevaCategoria = "plata"; nuevoPeso = 3; }
        } else if (cantidadMedios >= 2 || cantidadCompras >= 1) {
            if (nuevoPeso < 2) { nuevaCategoria = "especial"; nuevoPeso = 2; }
        }

        if (!nuevaCategoria.equals(categoriaActual)) {
            cliente.setCategoria(nuevaCategoria);
            clienteRepository.save(cliente);
        }
    }

    private int obtenerPesoCategoria(String categoria) {
        switch (categoria) {
            case "platino": return 5;
            case "oro": return 4;
            case "plata": return 3;
            case "especial": return 2;
            case "comun": default: return 1;
        }
    }
}
