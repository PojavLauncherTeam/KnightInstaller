/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package git.artdeell.knightinstaller;

/**
 *
 * @author maks
 */
public interface Progress {
    void postStepProgress(int prg);
    void postPartProgress(int prg);
    void postMaxSteps(int max);
    void postMaxPart(int max);
    void setPartIndeterminate(boolean indeterminate);
    void postLogLine(String line, Throwable th);
    void moveToTop();
    public void unlockExit();
}
