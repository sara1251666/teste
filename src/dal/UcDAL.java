package dal;

import model.UnidadeCurricular;
import model.Docente;
import model.Curso;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsável pelas operações de acesso a dados das Unidades Curriculares.
 */
public class UcDAL {
    private static final String NOME_FICHEIRO = "ucs.csv";
    private static final String CABECALHO = "sigla;nome;anoCurricular;siglaDocenteResponsavel;siglaCurso";

    /**
     * Devolve os dados crus de uma UC (uma linha do CSV dividida em array).
     */
    public static String[] obterDadosBrutosUC(String sigla, String pastaBase) {
        String caminho = pastaBase + File.separator + NOME_FICHEIRO;
        List<String> linhas = DALUtil.lerFicheiro(caminho);

        for (String linha : linhas) {
            if (linha.equalsIgnoreCase(CABECALHO)) continue;

            String[] dados = linha.split(";", -1);
            if (dados.length >= 4 && dados[0].trim().equalsIgnoreCase(sigla)) {
                return dados;
            }
        }
        return null;
    }
    /**
     * Conta quantas UCs existem num determinado curso e ano.
     */
    public static int contarUcsPorCursoEAno(String siglaCurso, int ano, String pastaBase) {
        String caminho = pastaBase + File.separator + NOME_FICHEIRO;
        List<String> linhas = DALUtil.lerFicheiro(caminho);
        int contagem = 0;

        for (String linha : linhas) {
            if (linha.equalsIgnoreCase(CABECALHO)) continue;

            String[] dados = linha.split(";", -1);
            if (dados.length >= 5) {
                try {
                    int anoCurricular = Integer.parseInt(dados[2].trim());
                    if (anoCurricular == ano && dados[4].trim().equalsIgnoreCase(siglaCurso)) {
                        contagem++;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return contagem;
    }

    public static UnidadeCurricular procurarUC(String sigla, String pastaBase) {
        String caminho = pastaBase + File.separator + NOME_FICHEIRO;
        List<String> linhas = DALUtil.lerFicheiro(caminho);
        UnidadeCurricular ucEncontrada = null;

        for (String linha : linhas) {
            String[] dados = linha.split(";", -1);

            if (dados.length >= 4 && dados[0].trim().equalsIgnoreCase(sigla)) {

                if (ucEncontrada == null) {
                    try {
                        int ano = Integer.parseInt(dados[2].trim());

                        String siglaDocente = dados[3].trim();
                        Docente doc = dal.DocenteDAL.procurarPorSigla(siglaDocente, pastaBase);

                        ucEncontrada = new UnidadeCurricular(dados[0].trim(), dados[1].trim(), ano, doc);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
                    if (dados.length >= 5 && !dados[4].trim().equalsIgnoreCase("N/A")) {
                    String siglaCurso = dados[4].trim();
                    Curso curso = dal.CursoDAL.procurarCurso(siglaCurso, pastaBase);

                    if (curso != null) {
                        ucEncontrada.adicionarCurso(curso);
                    }
                }
            }
        }
        return ucEncontrada;
    }


    /**
     * Devolve uma lista de siglas de UCs associadas a um docente específico.
     * (Muito útil para a DocenteBLL!)
     */
    public static List<String> obterSiglasUcsPorDocente(String siglaDocente, String pastaBase) {
        String caminho = pastaBase + File.separator + NOME_FICHEIRO;
        List<String> linhas = DALUtil.lerFicheiro(caminho);
        List<String> siglas = new ArrayList<>();

        for (String linha : linhas) {
            if (linha.equalsIgnoreCase(CABECALHO)) continue;

            String[] dados = linha.split(";", -1);
            if (dados.length >= 4 && dados[3].trim().equalsIgnoreCase(siglaDocente)) {
                siglas.add(dados[0].trim());
            }
        }
        return siglas;
    }

    public static void adicionarUC(UnidadeCurricular uc, String siglaCurso, String pastaBase) {
        if (uc == null) return;

        String caminho = pastaBase + File.separator + NOME_FICHEIRO;
        DALUtil.garantirFicheiroECabecalho(caminho, CABECALHO);

        String siglaDocente = (uc.getDocenteResponsavel() != null) ? uc.getDocenteResponsavel().getSigla() : "N/A";
        String cursoStr = (siglaCurso != null && !siglaCurso.isEmpty()) ? siglaCurso : "N/A";

        String linha = uc.getSigla() + ";" + uc.getNome() + ";" +
                uc.getAnoCurricular() + ";" + siglaDocente + ";" + cursoStr;

        DALUtil.adicionarLinhaCSV(caminho, linha);
    }

    public static void atualizarUC(UnidadeCurricular ucAtualizada, String siglaCurso, String pastaBase) {
        String caminho = pastaBase + File.separator + NOME_FICHEIRO;
        List<String> linhasAntigas = DALUtil.lerFicheiro(caminho);

        if (linhasAntigas.isEmpty()) return;

        List<String> linhasAtualizadas = new ArrayList<>();
        boolean atualizado = false;

        for (String linha : linhasAntigas) {
            if (linha.equalsIgnoreCase(CABECALHO)) {
                linhasAtualizadas.add(linha);
                continue;
            }

            String[] dados = linha.split(";", -1);
            if (dados.length > 0 && dados[0].trim().equalsIgnoreCase(ucAtualizada.getSigla())) {
                String siglaDocente = (ucAtualizada.getDocenteResponsavel() != null)
                        ? ucAtualizada.getDocenteResponsavel().getSigla() : "N/A";
                String cursoStr = (siglaCurso != null && !siglaCurso.isEmpty()) ? siglaCurso : "N/A";

                linhasAtualizadas.add(ucAtualizada.getSigla() + ";" + ucAtualizada.getNome() + ";" +
                        ucAtualizada.getAnoCurricular() + ";" + siglaDocente + ";" + cursoStr);
                atualizado = true;
            } else {
                linhasAtualizadas.add(linha);
            }
        }

        if (atualizado) {
            DALUtil.reescreverFicheiro(caminho, linhasAtualizadas);
        }
    }


}