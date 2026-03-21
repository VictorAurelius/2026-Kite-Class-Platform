'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { SECTION_SLOTS } from '@/lib/template/slots';
import type { SectionId } from '@/lib/template/types';
import { toast } from '@/hooks/use-toast';
import type { LandingPageContent } from '@/lib/cms/api/landing';

interface CMSEditorProps {
  tenantId: string;
  initialData?: LandingPageContent;
  onSave?: (data: LandingPageContent) => Promise<void>;
}

interface SlotFormData {
  [sectionId: string]: {
    [slotId: string]: string | string[];
  };
}

export function CMSEditor({ tenantId: _tenantId, initialData = {}, onSave }: CMSEditorProps) {
  const [isSaving, setIsSaving] = useState(false);
  const { register, handleSubmit, formState: { isDirty, errors } } = useForm<SlotFormData>({
    defaultValues: initialData as SlotFormData,
  });

  const handleSaveForm = async (data: SlotFormData) => {
    setIsSaving(true);
    try {
      if (onSave) {
        await onSave(data as LandingPageContent);
      }
      toast({
        title: 'Success',
        description: 'Landing page saved successfully',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to save landing page',
        variant: 'destructive',
      });
      console.error('Save error:', error);
    } finally {
      setIsSaving(false);
    }
  };

  // Get available sections (showing a subset for MVP)
  const sections: SectionId[] = ['hero', 'about', 'courses'];

  return (
    <div className="container mx-auto py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold">Landing Page Editor</h1>
        <p className="text-muted-foreground">Edit content for your landing page sections</p>
      </div>

      <form onSubmit={handleSubmit(handleSaveForm)} className="space-y-6">
        {sections.map((sectionId) => {
          const sectionSlots = SECTION_SLOTS[sectionId];
          if (!sectionSlots) return null;

          return (
            <Card key={sectionId}>
              <CardHeader>
                <CardTitle className="capitalize">{sectionId} Section</CardTitle>
                <CardDescription>
                  Edit the content for the {sectionId} section
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {sectionSlots.map((slot) => {
                  const fieldName = `${sectionId}.${slot.id}` as const;

                  return (
                    <div key={slot.id} className="space-y-2">
                      <Label htmlFor={fieldName}>
                        {slot.label}
                        {slot.required && <span className="text-destructive ml-1">*</span>}
                      </Label>

                      {slot.type === 'text' && (
                        <Input
                          id={fieldName}
                          {...register(fieldName, {
                            required: slot.required ? `${slot.label} is required` : false,
                            maxLength: slot.maxLength ? {
                              value: slot.maxLength,
                              message: `Maximum ${slot.maxLength} characters`,
                            } : undefined,
                          })}
                          placeholder={slot.placeholder}
                          maxLength={slot.maxLength}
                        />
                      )}

                      {slot.type === 'richtext' && (
                        <Textarea
                          id={fieldName}
                          {...register(fieldName, {
                            required: slot.required ? `${slot.label} is required` : false,
                          })}
                          placeholder={slot.placeholder}
                          rows={4}
                        />
                      )}

                      {slot.type === 'image' && (
                        <Input
                          id={fieldName}
                          type="url"
                          {...register(fieldName)}
                          placeholder="https://example.com/image.jpg"
                        />
                      )}

                      {slot.type === 'items' && (
                        <Textarea
                          id={fieldName}
                          {...register(fieldName)}
                          placeholder="Enter items, one per line"
                          rows={6}
                        />
                      )}

                      {errors[fieldName as keyof SlotFormData] && (
                        <p className="text-sm text-destructive">
                          {String(errors[fieldName as keyof SlotFormData]?.message || '')}
                        </p>
                      )}
                    </div>
                  );
                })}
              </CardContent>
            </Card>
          );
        })}

        <div className="flex justify-end gap-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => window.location.reload()}
            disabled={!isDirty}
          >
            Reset
          </Button>
          <Button
            type="submit"
            disabled={isSaving || !isDirty}
          >
            {isSaving ? 'Saving...' : 'Save Changes'}
          </Button>
        </div>
      </form>
    </div>
  );
}
