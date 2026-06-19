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
        return medioPagoRepository.findByClienteIdentificadorAndActivo(clienteId, "si");
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

        return tarjetaRepository.save(tarjeta);
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

        return cuentaRepository.save(cuenta);
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
        cheque.setNroCheque(req.getNroCheque());
        cheque.setBanco(req.getBanco());
        cheque.setMoneda(req.getMoneda());
        cheque.setMontoGarantia(req.getMontoGarantia());
        cheque.setFechaEntrega(req.getFechaEntrega());

        return chequeRepository.save(cheque);
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
            String monedaSubasta,
            Double importeComprometido) {
        MedioPago medio = obtenerMedioDelCliente(medioPagoId, cliente);
        validarActivoYMoneda(medio, monedaSubasta);

        BigDecimal requerido = BigDecimal.valueOf(
                importeComprometido == null ? 0.0 : importeComprometido);
        BigDecimal comprometido = BigDecimal.valueOf(valor(
                registroSubastaRepository.sumPendienteByMedioPagoId(medioPagoId)));

        if (medio instanceof CuentaBancaria cuenta) {
            BigDecimal disponible = saldo(cuenta.getFondosReservados()).subtract(comprometido);
            if (disponible.compareTo(requerido) < 0) {
                throw new RuntimeException("403: Fondos reservados insuficientes");
            }
        } else if (medio instanceof ChequeCertificado cheque) {
            if (!"si".equalsIgnoreCase(cheque.getVerificadoCheque())) {
                throw new RuntimeException("403: El cheque no esta verificado");
            }
            BigDecimal disponible = saldo(cheque.getMontoGarantia()).subtract(comprometido);
            if (disponible.compareTo(requerido) < 0) {
                throw new RuntimeException("403: Garantia del cheque insuficiente");
            }
        }
        return medio;
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

    private double valor(Double numero) {
        return numero == null ? 0.0 : numero;
    }
}
