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

        TarjetaCredito tarjeta = new TarjetaCredito();
        tarjeta.setCliente(cliente);
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

        CuentaBancaria cuenta = new CuentaBancaria();
        cuenta.setCliente(cliente);
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

        ChequeCertificado cheque = new ChequeCertificado();
        cheque.setCliente(cliente);
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