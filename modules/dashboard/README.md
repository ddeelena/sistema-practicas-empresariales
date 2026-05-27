# Dashboard (Panel de Inicio)

Este módulo implementa el **patrón Service** y sigue los principios **SOLID**:

* **S**ingle Responsibility – solo expone métricas del sistema.
* **O**pen/Closed – la clase `DashboardServiceImpl` puede extenderse añadiendo nuevas métricas sin modificar el código existente.
* **L**iskov Substitution & **I**nterface Segregation – implementa la interfaz `DashboardService`.
* **D**ependency Inversion – los controladores dependen de la abstracción `DashboardService`.

El componente está registrado como `@Service` en Spring y se consume desde el controlador o desde otras capas del backend.
