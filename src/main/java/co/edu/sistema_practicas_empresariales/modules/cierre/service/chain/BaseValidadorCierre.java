package co.edu.sistema_practicas_empresariales.modules.cierre.service.chain;

import co.edu.sistema_practicas_empresariales.modules.practica.model.Practica;

public abstract class BaseValidadorCierre implements ValidadorCierre {
    protected final ValidadorCierre siguiente;

    protected BaseValidadorCierre(ValidadorCierre siguiente) {
        this.siguiente = siguiente;
    }

    protected void verificarSiguiente(Practica practica) {
        if (siguiente != null) {
            siguiente.validar(practica);
        }
    }
}
