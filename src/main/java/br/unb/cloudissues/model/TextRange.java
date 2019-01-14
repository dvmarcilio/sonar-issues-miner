package br.unb.cloudissues.model;

public class TextRange {

	private Long startLine;

	private Long endLine;

	private Long startOffset;

	private Long endOffset;

	public Long getStartLine() {
		return startLine;
	}

	public void setStartLine(Long startLine) {
		this.startLine = startLine;
	}

	public Long getEndLine() {
		return endLine;
	}

	public void setEndLine(Long endLine) {
		this.endLine = endLine;
	}

	public Long getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(Long startOffset) {
		this.startOffset = startOffset;
	}

	public Long getEndOffset() {
		return endOffset;
	}

	public void setEndOffset(Long endOffset) {
		this.endOffset = endOffset;
	}

	@Override
	public String toString() {
		return "TextRange [startLine=" + startLine + ", endLine=" + endLine + ", startOffset=" + startOffset
				+ ", endOffset=" + endOffset + "]";
	}

}
