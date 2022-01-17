package cilabo.labo.developing.multitasking;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.fileoutput.FileOutputContext;

import cilabo.util.fileoutput.SolutionListOutputFormat;

public class MultiTaskingSolutionListOutput extends SolutionListOutputFormat {

	@Override
	public void printSolutionList(FileOutputContext fileContext, List<? extends Solution<?>> solutionList) {
		BufferedWriter bufferedWriter = fileContext.getFileWriter();
		try {
			if(solutionList.size() > 0) {
				for(int i = 0; i < solutionList.size(); i++) {
					bufferedWriter.write("test");
				}
				bufferedWriter.close();
			}
		}
		catch (IOException e) {
			throw new JMetalException("Error writing data ", e);
		}

	}


}
