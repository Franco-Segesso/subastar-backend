package com.grupo6.subastar.service;

import com.grupo6.subastar.dto.*;
import com.grupo6.subastar.model.*;
import com.grupo6.subastar.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    // DELETE: baja lógica
    @Transactional
    public void darDeBaja(Integer medioPagoId) throws Exception {
        MedioPago medio = medioPagoRepository.findById(medioPagoId)
                .orElseThrow(() -> new Exception("Medio de pago no encontrado con id: " + medioPagoId));
        medio.setActivo("no");
        medioPagoRepository.save(medio);
    }
}