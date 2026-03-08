/**
 * CSV export utilities.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

/**
 * Convert data array to CSV string.
 *
 * @param data - Array of objects to convert
 * @param headers - Array of header names (object keys to extract)
 * @returns CSV string
 */
export function convertToCSV(
  data: Record<string, any>[],
  headers: string[]
): string {
  if (!data || data.length === 0) {
    return headers.join(',');
  }

  // CSV header row
  const csvHeaders = headers.join(',');

  // CSV data rows
  const csvRows = data.map((row) =>
    headers
      .map((header) => {
        const value = row[header] ?? '';
        const stringValue = String(value);

        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (
          stringValue.includes(',') ||
          stringValue.includes('"') ||
          stringValue.includes('\n')
        ) {
          return `"${stringValue.replace(/"/g, '""')}"`;
        }

        return stringValue;
      })
      .join(',')
  );

  return [csvHeaders, ...csvRows].join('\n');
}

/**
 * Download CSV file to user's computer.
 *
 * @param csv - CSV string content
 * @param filename - Name of file to download (without extension)
 */
export function downloadCSV(csv: string, filename: string): void {
  // Add .csv extension if not present
  const finalFilename = filename.endsWith('.csv')
    ? filename
    : `${filename}.csv`;

  // Create blob with UTF-8 BOM for Excel compatibility
  const BOM = '\uFEFF';
  const blob = new Blob([BOM + csv], { type: 'text/csv;charset=utf-8;' });

  // Create download link and trigger download
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);

  link.setAttribute('href', url);
  link.setAttribute('download', finalFilename);
  link.style.visibility = 'hidden';

  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  // Cleanup
  URL.revokeObjectURL(url);
}

/**
 * Export data to CSV file (convenience function combining convert + download).
 *
 * @param data - Array of objects to export
 * @param headers - Array of header names
 * @param filename - Name of file to download
 */
export function exportToCSV(
  data: Record<string, any>[],
  headers: string[],
  filename: string
): void {
  const csv = convertToCSV(data, headers);
  downloadCSV(csv, filename);
}
