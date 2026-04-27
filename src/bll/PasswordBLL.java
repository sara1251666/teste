package bll;

import dal.CredencialDAL;
import utils.SegurancaPasswords;
import utils.PasswordGenerator;
import utils.EmailService;

/**
 * Lógica de negócio para a gestão de palavras-passe.
 * Gera uma nova password segura, aplica hashing PBKDF2,
 * persiste a credencial e envia a nova password por email ao utilizador.
 */
public class PasswordBLL {

    private static final String PASTA_BD = "bd";

    /**
     * Recupera a password de um utilizador substituindo-a por uma nova.
     * @param email Email do utilizador que pediu a recuperação.
     * @return true se o email existir e a operação for bem-sucedida.
     */
    public boolean recuperarPassword(String email) {
        String[] creds = CredencialDAL.obterCredenciais(email, PASTA_BD);
        if (creds == null) return false;

        String novaPassLimpa  = PasswordGenerator.gerarPasswordSegura();
        String novaPassSegura = SegurancaPasswords.gerarCredencialMista(novaPassLimpa);
        CredencialDAL.atualizarPassword(email, novaPassSegura, PASTA_BD);
        EmailService.enviarRecuperacaoPassword("Utilizador", email, novaPassLimpa);
        return true;
    }
}