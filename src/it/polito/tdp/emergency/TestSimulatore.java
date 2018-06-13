package it.polito.tdp.emergency;

public class TestSimulatore {

	public static void main(String[] args) {
		
		Simulatore sim = new Simulatore();
		sim.setNP(200);
		sim.setT_ARRIVAL(5);
		sim.setNS(5);
		sim.init();
		
		sim.run();
		
		System.out.println("Pazienti curati: "+ sim.getPaz_curati());
		System.out.println("Pazienti abbandonati: "+ sim.getPaz_abbandonati());
		System.out.println("Pazienti morti: "+ sim.getPaz_morti());

	}

}
