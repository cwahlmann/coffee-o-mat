package de.dreierschach.akka.coffeeomat.common;

public enum Kaffeesorte {
	KaffeKlein(Zutat.Bohnen, 50, 1.75f), KaffeeMittel(Zutat.Bohnen, 75, 2.49f), KaffeeGross(Zutat.Bohnen, 120,
			3.30f), Espresso(Zutat.Bohnen, 50, Zutat.Zucker, 20,
					2.25f), LatteMacchiato(Zutat.Bohnen, 60, Zutat.Milch, 200, 3.79f);

	private final Zutat[] zutaten;
	private final int mengen[];
	private final float preis;

	private Kaffeesorte(Zutat zutat, int menge, float preis) {
		this.zutaten = new Zutat[] { zutat };
		this.mengen = new int[] { menge };
		this.preis = preis;
	}

	private Kaffeesorte(Zutat zutat1, int menge1, Zutat zutat2, int menge2, float preis) {
		this.zutaten = new Zutat[] { zutat1, zutat2 };
		this.mengen = new int[] { menge1, menge2 };
		this.preis = preis;
	}

	public Zutat[] zutat() {
		return zutaten;
	}

	public int[] menge() {
		return mengen;
	}

	public float preis() {
		return preis;
	}
}
